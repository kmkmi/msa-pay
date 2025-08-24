package com.msapay.money.service;

import com.msapay.common.CountDownLatchManager;
import com.msapay.common.RechargingMoneyTask;
import com.msapay.common.SubTask;
import com.msapay.common.UseCase;
import com.msapay.money.domain.RegisteredBankAccountAggregateIdentifier;
import com.msapay.money.persistence.MemberMoneyJpaEntity;
import com.msapay.money.persistence.MemberMoneyMapper;
import com.msapay.money.persistence.MoneyChangingRequestMapper;
import com.msapay.money.service.port.*;
import com.msapay.money.domain.MemberMoney;
import com.msapay.money.controller.request.MoneyChangingRequest;
import com.msapay.money.controller.command.CreateMemberMoneyCommand;
import com.msapay.money.controller.command.FindMemberMoneyListByMembershipIdsCommand;
import com.msapay.money.controller.command.IncreaseMoneyRequestCommand;
import com.msapay.money.service.usecase.CreateMemberMoneyUseCase;
import com.msapay.money.service.usecase.IncreaseMoneyRequestUseCase;
import com.msapay.money.domain.repository.MoneyAggregateRepository;
import com.msapay.money.domain.MoneyAggregate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.msapay.money.service.port.UpdateMoneyChangingRequestStatusPort;

import javax.transaction.Transactional;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.CountDownLatch;

@Slf4j
@UseCase
@RequiredArgsConstructor
@Transactional
public class IncreaseMoneyRequestService implements IncreaseMoneyRequestUseCase, CreateMemberMoneyUseCase {

    private final CountDownLatchManager countDownLatchManager;
    private final SendRechargingMoneyTaskPort sendRechargingMoneyTaskPort;
    private final GetMembershipPort membershipPort;
    private final BankingServicePort bankingServicePort;
    private final IncreaseMoneyPort increaseMoneyPort;
    private final MoneyChangingRequestMapper mapper;
    private final MemberMoneyMapper memberMoneyMapper;
    private final CreateMemberMoneyPort createMemberMoneyPort;
    private final GetMemberMoneyListPort getMemberMoneyListPort;
    private final MoneyAggregateRepository moneyAggregateRepository;
    private final UpdateMoneyChangingRequestStatusPort updateMoneyChangingRequestStatusPort;

    @Override
    public MoneyChangingRequest increaseMoneyRequest(IncreaseMoneyRequestCommand command) {

        try {
            // 머니의 충전.증액이라는 과정
            // 1. 고객 정보가 정상인지 확인 (멤버)
            if (!membershipPort.getMembership(command.getTargetMembershipId()).isValid()){
                return createMoneyChangingRequest(command, null, 2);
            }

            // 2. 고객의 연동된 계좌가 있는지, 고객의 연동된 계좌의 잔액이 충분한지도 확인 (뱅킹)
            RegisteredBankAccountAggregateIdentifier bankAccount = bankingServicePort.getRegisteredBankAccount(command.getTargetMembershipId());
            if (null == bankAccount) {
                return createMoneyChangingRequest(command, null, 2);
            }
            if(command.getAmount() > bankingServicePort.getBanckAccountBalance(bankAccount.getBankName(), bankAccount.getBankAccountNumber())){
                return createMoneyChangingRequest(command, null, 2);
            }

            // 3. 법인 계좌 상태도 정상인지 확인 (뱅킹)
            if (!bankingServicePort.verifyCorpAccount()) {
                return createMoneyChangingRequest(command, null, 2);
            }

            // 4. 머니 어그리게이트 생성 또는 조회
            String aggregateId = "money-" + command.getTargetMembershipId();
            MoneyAggregate aggregate = getOrCreateMoneyAggregate(aggregateId, command.getTargetMembershipId());

            // 5. 머니 증가 요청 이벤트 발생
            String taskId = "sync-" + UUID.randomUUID();
            aggregate.requestMoneyIncrease(command.getAmount(), taskId);

            // 6. 어그리게이트 저장 (이벤트 저장)
            moneyAggregateRepository.save(aggregate).join();

            // 7. 증액을 위한 "기록". 요청 상태로 MoneyChangingRequest 를 생성한다. (MoneyChangingRequest)
            MoneyChangingRequest pendingRequest = createMoneyChangingRequest(command, taskId, 0); // 0: 대기 중

            // 8. 펌뱅킹을 수행하고 (고객의 연동된 계좌 -> 패캠페이 법인 계좌) (뱅킹)
            boolean remittanceResult = bankingServicePort.requestFirmbanking(bankAccount.getBankName(), bankAccount.getBankAccountNumber(), command.getAmount());
            if (!remittanceResult) {
                return createMoneyChangingRequest(command, taskId, 2);
            }

            // 9. 머니 증가 처리
            MemberMoneyJpaEntity memberMoneyJpaEntity = increaseMoneyPort.increaseMoney(
                    new MemberMoney.MembershipId(command.getTargetMembershipId()),
                    command.getAmount()
            );

            if (memberMoneyJpaEntity != null) {
                // 머니 증가 완료 이벤트 발생
                // 결과가 정상적이라면. 성공으로 MoneyChangingRequest 상태값을 변동 후에 리턴
                // 성공 시에 멤버의 MemberMoney 값 증액이 필요해요

                aggregate.completeMoneyIncrease(command.getAmount(), taskId);
                moneyAggregateRepository.save(aggregate).join();

                // MoneyChangingRequest 생성 및 반환
                return createMoneyChangingRequest(command, taskId, 1); // 1: 성공
            }

            // 머니 증가 실패 이벤트 발생
            // 결과가 실패라면, 실패라고 MoneyChangingRequest 상태값을 변동 후에 리턴
            aggregate.failMoneyIncrease(command.getAmount(), taskId, "Money increase operation failed");
            moneyAggregateRepository.save(aggregate).join();

            return createMoneyChangingRequest(command, taskId, 2); // 2: 실패

        } catch (Exception e) {
            log.error("Failed to process money increase request: {}", command.getTargetMembershipId(), e);
            throw new RuntimeException("Money increase request failed", e);
        }
    }

    @Override
    public MoneyChangingRequest increaseMoneyRequestAsync(IncreaseMoneyRequestCommand command) {
        try {
            log.info("Starting async money increase request for membership: {}, amount: {}", 
                command.getTargetMembershipId(), command.getAmount());
            
            // 1. 머니 어그리게이트 생성 또는 조회
            String aggregateId = "money-" + command.getTargetMembershipId();
            MoneyAggregate aggregate = getOrCreateMoneyAggregate(aggregateId, command.getTargetMembershipId());
            log.info("Money aggregate ready: {}", aggregateId);

            // 2. 머니 증가 요청 이벤트 발생
            String taskId = "async-" + UUID.randomUUID();
            aggregate.requestMoneyIncrease(command.getAmount(), taskId);
            log.info("Money increase event created for task: {}", taskId);

            // 3. 어그리게이트 저장 (이벤트 저장)
            moneyAggregateRepository.save(aggregate).join();
            log.info("Aggregate saved with events for task: {}", taskId);

            // 4. 태스크 생성 및 Kafka 전송
            RechargingMoneyTask task = createTask(command, taskId);
            log.info("Task created: {} with {} subTasks", taskId, task.getSubTaskList().size());
            
            sendRechargingMoneyTaskPort.sendRechargingMoneyTaskPort(task);
            log.info("Task sent to Kafka topic for task: {}", taskId);

            // 5. 즉시 응답 - 대기 중인 상태
            MoneyChangingRequest pendingRequest = createMoneyChangingRequest(command, taskId, 0); // 0: 대기 중
            log.info("Created pending request for task: {} with status: PENDING", taskId);

            // 6. 비동기로 결과 처리 시작
            processTaskResultAsync(aggregate, task, command);
            log.info("Async result processing started for task: {}", taskId);

            return pendingRequest;

        } catch (Exception e) {
            log.error("Failed to process async money increase request: {}", command.getTargetMembershipId(), e);
            throw new RuntimeException("Async money increase request failed", e);
        }
    }

    private MoneyAggregate getOrCreateMoneyAggregate(String aggregateId, String membershipId) {
        return moneyAggregateRepository.findById(aggregateId)
                .thenApply(existingAggregate -> {
                    if (existingAggregate != null) {
                        return existingAggregate;
                    }
                    return new MoneyAggregate(aggregateId, membershipId);
                })
                .join();
    }

    private MoneyChangingRequest createMoneyChangingRequest(IncreaseMoneyRequestCommand command, String taskId, int status) {
        log.info("Successfully created MoneyChangingRequest for task: {} to status: {}", taskId, status);
        return mapper.mapToDomainEntity(increaseMoneyPort.createMoneyChangingRequest(
                new MoneyChangingRequest.TargetMembershipId(command.getTargetMembershipId()),
                new MoneyChangingRequest.MoneyChangingType(1),
                new MoneyChangingRequest.ChangingMoneyAmount(command.getAmount()),
                new MoneyChangingRequest.MoneyChangingStatus(status),
                new MoneyChangingRequest.Uuid(taskId)
        ));
    }

    private RechargingMoneyTask createTask(IncreaseMoneyRequestCommand command, String taskId) {
        List<SubTask> subTaskList = new ArrayList<>();

        subTaskList.add(SubTask.builder()
                .subTaskName("validMemberTask : 멤버십 유효성 검사")
                .taskType("membership")
                .status("ready")
                .build());

        subTaskList.add(SubTask.builder()
                .subTaskName("validBankingAccountTask : 뱅킹 계좌 유효성 검사")
                .taskType("banking")
                .status("ready")
                .build());

        subTaskList.add(SubTask.builder()
                .subTaskName("validCorpAccountTask : 법인 계좌 유효성 검사")
                .taskType("corpAccount")
                .status("ready")
                .build());

        return RechargingMoneyTask.builder()
                .taskID(taskId)
                .taskName("Increase Money Task / 머니 충전 Task")
                .subTaskList(subTaskList)
                .moneyAmount(command.getAmount())
                .membershipID(command.getTargetMembershipId())
                .toBankName("test")
                .build();
    }

    private void processTaskResultAsync(MoneyAggregate aggregate, RechargingMoneyTask task, IncreaseMoneyRequestCommand command) {
        CompletableFuture.runAsync(() -> {
            String taskId = task.getTaskID();
            try {
                log.info("Starting async task result processing for task: {}", taskId);
                log.info("Task details - Membership: {}, Amount: {}, SubTasks: {}", 
                    task.getMembershipID(), task.getMoneyAmount(), task.getSubTaskList().size());

                // CountDownLatch 등록 (이미 존재하는 경우 덮어쓰기)
                if (countDownLatchManager.hasKey(taskId)) {
                    log.warn("CountDownLatch already exists for task: {}, removing old one", taskId);
                    countDownLatchManager.removeData(taskId);
                }
                countDownLatchManager.addCountDownLatch(taskId);
                log.info("CountDownLatch registered for task: {}", taskId);

                // 태스크 결과 대기
                log.info("Waiting for task result for task: {} (timeout: 30s)", taskId);
                String result = waitForTaskResult(taskId);
                log.info("Task result received for task: {} - {}", taskId, result);

                // 결과 처리
                log.info("Processing task result for task: {}", taskId);
                processTaskResult(result, aggregate, task, command);

                // 완료 후 정리
                countDownLatchManager.removeData(taskId);
                log.info("Async task result processing completed for task: {}", taskId);

            } catch (Exception e) {
                log.error("Failed to process async task result for task: {}", taskId, e);
                handleTaskProcessingError(aggregate, task, command, e);
                
                // 에러 발생 시에도 정리
                countDownLatchManager.removeData(taskId);
                log.info("Cleaned up resources for failed task: {}", taskId);
            }
        });
    }

    private String waitForTaskResult(String taskId) {
        try {
            // CountDownLatch가 준비될 때까지 잠시 대기 (최대 5초)
            int maxWaitForLatch = 50; // 5초
            CountDownLatch latch = null;
            
            for (int i = 0; i < maxWaitForLatch; i++) {
                latch = countDownLatchManager.getCountDownLatch(taskId);
                if (latch != null) {
                    break;
                }
                Thread.sleep(100); // 100ms 대기
            }
            
            if (latch == null) {
                log.error("CountDownLatch not found for task: {} after waiting 5 seconds", taskId);
                throw new RuntimeException("CountDownLatch not found for task: " + taskId);
            }
            
            // 60초 타임아웃 설정 (30초에서 증가)
            boolean completed = latch.await(60, TimeUnit.SECONDS);
            if (!completed) {
                log.error("Task result waiting timed out for task: {} after 60 seconds", taskId);
                throw new RuntimeException("Task result waiting timed out for task: " + taskId);
            }
            
            String result = countDownLatchManager.getDataForKey(taskId);
            if (result == null) {
                log.error("Task result data not found for task: {}", taskId);
                throw new RuntimeException("Task result data not found for task: " + taskId);
            }
            
            return result;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Task result waiting interrupted for task: " + taskId, e);
        }
    }

    private void processTaskResult(String result, MoneyAggregate aggregate, RechargingMoneyTask task, IncreaseMoneyRequestCommand command) {
        if (result.equals("success")) {
            // 서브태스크 성공 시 - Saga가 시작되어 실행 중
            String sagaId = task.getSagaId();
            log.info("Sub-tasks completed successfully for task: {} with sagaId: {}, Saga execution in progress", 
                task.getTaskID(), sagaId);
            
            // Saga가 실행 중이므로 여기서는 아무것도 하지 않음
            // saga-orchestrator에서 펌뱅킹 성공 시 머니 증가 이벤트를 발행할 것임
            
        } else {
            // 서브태스크 실패 시
            log.warn("Sub-tasks failed for task: {}, result: {}", task.getTaskID(), result);

            // 머니 증가 실패 이벤트 발생
            aggregate.failMoneyIncrease(command.getAmount(), task.getTaskID(), "Sub-tasks failed: " + result);
            moneyAggregateRepository.save(aggregate).join();

            updateMoneyChangingRequestStatus(task.getTaskID(), 2); // 2: 실패
        }
    }

    private void handleTaskProcessingError(MoneyAggregate aggregate, RechargingMoneyTask task, IncreaseMoneyRequestCommand command, Exception e) {
        log.error("Task processing error for task: {}", task.getTaskID(), e);

        // 머니 증가 실패 이벤트 발생
        aggregate.failMoneyIncrease(command.getAmount(), task.getTaskID(), "Task processing error: " + e.getMessage());
        moneyAggregateRepository.save(aggregate).join();

        updateMoneyChangingRequestStatus(task.getTaskID(), 2); // 2: 실패
    }

    private void updateMoneyChangingRequestStatus(String taskId, int status) {
        try {
            // MoneyChangingRequest 상태 업데이트 로직 구현
            boolean updateResult = updateMoneyChangingRequestStatusPort.updateMoneyChangingRequestStatus(taskId, status);
            if (updateResult) {
                log.info("Successfully updated MoneyChangingRequest status for task: {} to status: {}", taskId, status);
            } else {
                log.warn("Failed to update MoneyChangingRequest status for task: {} to status: {}", taskId, status);
            }
        } catch (Exception e) {
            log.error("Failed to update MoneyChangingRequest status for task: {}", taskId, e);
        }
    }

    @Override
    public void createMemberMoney(CreateMemberMoneyCommand command) {
        try {
            // 이벤트 소싱 기반으로 멤버 머니 생성
            String aggregateId = "money-" + command.getMembershipId();
            MoneyAggregate aggregate = new MoneyAggregate(aggregateId, command.getMembershipId());

            // 멤버 머니 생성 이벤트 발생
            aggregate.createMemberMoney(command.getMembershipId());

            // 어그리게이트 저장 (이벤트 저장)
            moneyAggregateRepository.save(aggregate).join();

            // 기존 포트를 통한 멤버 머니 생성
            createMemberMoneyPort.createMemberMoney(
                    new MemberMoney.MembershipId(command.getMembershipId()),
                    new MemberMoney.MoneyAggregateIdentifier(aggregateId)
            );

            log.info("Member money created successfully with aggregate ID: {}", aggregateId);

        } catch (Exception e) {
            log.error("Failed to create member money for membership: {}", command.getMembershipId(), e);
            throw new RuntimeException("Member money creation failed", e);
        }
    }

    @Override
    public List<MemberMoney> findMemberMoneyListByMembershipIds(FindMemberMoneyListByMembershipIdsCommand command) {
        // 여러개의 membership Ids 를 기준으로, memberMoney 정보를 가져와야 해요.
        List<MemberMoneyJpaEntity> memberMoneyJpaEntityList = getMemberMoneyListPort.getMemberMoneyPort(command.getMembershipIds());
        List<MemberMoney> memberMoneyList = new ArrayList<>();

        for(MemberMoneyJpaEntity memberMoneyJpaEntity : memberMoneyJpaEntityList) {
            memberMoneyList.add(memberMoneyMapper.mapToDomainEntity(memberMoneyJpaEntity));
        }

        return memberMoneyList;
    }
}
