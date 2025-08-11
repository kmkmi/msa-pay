package com.msapay.money.service;

import com.msapay.common.CountDownLatchManager;
import com.msapay.common.RechargingMoneyTask;
import com.msapay.common.SubTask;
import com.msapay.common.UseCase;
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
import com.msapay.common.SagaManager;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.transaction.Transactional;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@UseCase
@Primary
@RequiredArgsConstructor
@Transactional
public class IncreaseMoneyRequestService implements IncreaseMoneyRequestUseCase, CreateMemberMoneyUseCase {

    private static final Logger logger = LoggerFactory.getLogger(IncreaseMoneyRequestService.class);

    private final CountDownLatchManager countDownLatchManager;
    private final SendRechargingMoneyTaskPort sendRechargingMoneyTaskPort;
    private final GetMembershipPort membershipPort;
    private final IncreaseMoneyPort increaseMoneyPort;
    private final MoneyChangingRequestMapper mapper;
    private final MemberMoneyMapper memberMoneyMapper;
//    private final CommandGateway commandGateway;
    private final CreateMemberMoneyPort createMemberMoneyPort;
    private final GetMemberMoneyPort getMemberMoneyPort;

    private final GetMemberMoneyListPort getMemberMoneyListPort;
    private final SagaManager sagaManager;
    private final MoneyAggregateRepository moneyAggregateRepository;

    @Override
    public MoneyChangingRequest increaseMoneyRequest(IncreaseMoneyRequestCommand command) {

        // 머니의 충전.증액이라는 과정
        // 1. 고객 정보가 정상인지 확인 (멤버)
        membershipPort.getMembership(command.getTargetMembershipId());

        // 2. 고객의 연동된 계좌가 있는지, 고객의 연동된 계좌의 잔액이 충분한지도 확인 (뱅킹)

        // 3. 법인 계좌 상태도 정상인지 확인 (뱅킹)

        // 4. 증액을 위한 "기록". 요청 상태로 MoneyChangingRequest 를 생성한다. (MoneyChangingRequest)

        // 5. 펌뱅킹을 수행하고 (고객의 연동된 계좌 -> 패캠페이 법인 계좌) (뱅킹)

        // 6-1. 결과가 정상적이라면. 성공으로 MoneyChangingRequest 상태값을 변동 후에 리턴
        // 성공 시에 멤버의 MemberMoney 값 증액이 필요해요
        MemberMoneyJpaEntity memberMoneyJpaEntity = increaseMoneyPort.increaseMoney(
                new MemberMoney.MembershipId(command.getTargetMembershipId())
                ,command.getAmount());

        if(memberMoneyJpaEntity != null) {
            return mapper.mapToDomainEntity(increaseMoneyPort.createMoneyChangingRequest(
                            new MoneyChangingRequest.TargetMembershipId(command.getTargetMembershipId()),
                            new MoneyChangingRequest.MoneyChangingType(1),
                            new MoneyChangingRequest.ChangingMoneyAmount(command.getAmount()),
                            new MoneyChangingRequest.MoneyChangingStatus(1),
                            new MoneyChangingRequest.Uuid(UUID.randomUUID().toString())
                    )
            );
        }

        // 6-2. 결과가 실패라면, 실패라고 MoneyChangingRequest 상태값을 변동 후에 리턴
        return null;
    }

    @Override
    public MoneyChangingRequest increaseMoneyRequestAsync(IncreaseMoneyRequestCommand command) {
        // 1. Subtask, Task 생성
        RechargingMoneyTask task = createTask(command);
        
        // 2. Kafka로 태스크 전송
        sendRechargingMoneyTaskPort.sendRechargingMoneyTaskPort(task);
        logger.info("Task sent to Kafka: {}", task.getTaskID());
        
        // 3. 즉시 응답 - 태스크 ID를 포함한 대기 중인 응답 생성
        MoneyChangingRequest pendingRequest = createPendingMoneyChangingRequest(command, task.getTaskID());
        
        // 4. 비동기로 결과 처리 시작 (백그라운드에서 실행)
        processTaskResultAsync(task, command);
        
        return pendingRequest;
    }
    
    /**
     * 태스크 생성
     */
    private RechargingMoneyTask createTask(IncreaseMoneyRequestCommand command) {
        // Subtask 생성
        SubTask validMemberTask = SubTask.builder()
                .subTaskName("validMemberTask : " + "멤버십 유효성 검사")
                .membershipID(command.getTargetMembershipId())
                .taskType("membership")
                .status("ready")
                .build();

        SubTask validBankingAccountTask = SubTask.builder()
                .subTaskName("validBankingAccountTask : " + "뱅킹 계좌 유효성 검사")
                .membershipID(command.getTargetMembershipId())
                .taskType("banking")
                .status("ready")
                .build();

        List<SubTask> subTaskList = new ArrayList<>();
        subTaskList.add(validMemberTask);
        subTaskList.add(validBankingAccountTask);

        return RechargingMoneyTask.builder()
                .taskID(UUID.randomUUID().toString())
                .taskName("Increase Money Task / 머니 충전 Task")
                .subTaskList(subTaskList)
                .moneyAmount(command.getAmount())
                .membershipID(command.getTargetMembershipId())
                .toBankName("test")
                .build();
    }
    
    /**
     * 대기 중인 MoneyChangingRequest 생성 (즉시 응답용)
     */
    private MoneyChangingRequest createPendingMoneyChangingRequest(IncreaseMoneyRequestCommand command, String taskId) {
        return mapper.mapToDomainEntity(increaseMoneyPort.createMoneyChangingRequest(
                new MoneyChangingRequest.TargetMembershipId(command.getTargetMembershipId()),
                new MoneyChangingRequest.MoneyChangingType(1),
                new MoneyChangingRequest.ChangingMoneyAmount(command.getAmount()),
                new MoneyChangingRequest.MoneyChangingStatus(0), // 0: 대기 중
                new MoneyChangingRequest.Uuid(taskId)
        ));
    }
    
    /**
     * 비동기로 태스크 결과 처리
     */
    private void processTaskResultAsync(RechargingMoneyTask task, IncreaseMoneyRequestCommand command) {
        CompletableFuture.runAsync(() -> {
            try {
                logger.info("Starting async task result processing for task: {}", task.getTaskID());
                
                // CountDownLatch 등록
                countDownLatchManager.addCountDownLatch(task.getTaskID());
                
                // 태스크 결과 대기 (비동기)
                String result = waitForTaskResult(task.getTaskID());
                
                // 결과 처리
                processTaskResult(result, task, command);
                
            } catch (Exception e) {
                logger.error("Failed to process async task result for task: {}", task.getTaskID(), e);
                // 실패 시 적절한 에러 처리
                handleTaskProcessingError(task, command, e);
            }
        });
    }
    
    /**
     * 태스크 결과 대기
     */
    private String waitForTaskResult(String taskId) {
        try {
            countDownLatchManager.getCountDownLatch(taskId).await();
            return countDownLatchManager.getDataForKey(taskId);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Task result waiting interrupted for task: " + taskId, e);
        }
    }
    
    /**
     * 태스크 결과 처리
     */
    private void processTaskResult(String result, RechargingMoneyTask task, IncreaseMoneyRequestCommand command) {
        if (result.equals("success")) {
            // 비동기 태스크 성공 시 Saga 시작
            String sagaId = sagaManager.beginIncreaseMoneySaga(task);
            logger.info("Increase Money Saga started with ID: {} for task: {} after successful async task", sagaId, task.getTaskID());
            
            // Saga ID를 태스크에 연결하여 추적
            task.setSagaId(sagaId);
            
            // Saga 상태 업데이트
            sagaManager.updateSagaStatus(sagaId, "SUCCESS", "All subtasks completed successfully");
            
            // 머니 증가 처리
            MemberMoneyJpaEntity memberMoneyJpaEntity = increaseMoneyPort.increaseMoney(
                    new MemberMoney.MembershipId(command.getTargetMembershipId())
                    , command.getAmount());

            if (memberMoneyJpaEntity != null) {
                // Saga 완료
                sagaManager.completeSaga(sagaId, true, "Money increased successfully");
                
                // MoneyChangingRequest 상태를 성공으로 업데이트
                updateMoneyChangingRequestStatus(task.getTaskID(), 1); // 1: 성공
                
                logger.info("Money increase completed successfully for task: {}", task.getTaskID());
            } else {
                // 머니 증가 실패 시 Saga 실패 처리
                sagaManager.completeSaga(sagaId, false, "Money increase failed");
                updateMoneyChangingRequestStatus(task.getTaskID(), 2); // 2: 실패
                
                logger.error("Money increase failed for task: {}", task.getTaskID());
            }
        } else {
            // 비동기 태스크 실패 시
            logger.warn("Async task failed for task: {}, result: {}", task.getTaskID(), result);
            updateMoneyChangingRequestStatus(task.getTaskID(), 2); // 2: 실패
        }
    }
    
    /**
     * 태스크 처리 에러 핸들링
     */
    private void handleTaskProcessingError(RechargingMoneyTask task, IncreaseMoneyRequestCommand command, Exception e) {
        logger.error("Task processing error for task: {}", task.getTaskID(), e);
        updateMoneyChangingRequestStatus(task.getTaskID(), 2); // 2: 실패
        
        // 에러 상황에 대한 추가 처리 (로깅, 알림 등)
        // TODO: 에러 알림 시스템 연동
    }
    
    /**
     * MoneyChangingRequest 상태 업데이트
     */
    private void updateMoneyChangingRequestStatus(String taskId, int status) {
        try {
            // TODO: MoneyChangingRequest 상태 업데이트 로직 구현
            // 현재는 로깅만 수행
            logger.info("Updating MoneyChangingRequest status for task: {} to status: {}", taskId, status);
        } catch (Exception e) {
            logger.error("Failed to update MoneyChangingRequest status for task: {}", taskId, e);
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
            
            logger.info("Member money created successfully with aggregate ID: {}", aggregateId);
            
        } catch (Exception e) {
            logger.error("Failed to create member money for membership: {}", command.getMembershipId(), e);
            throw new RuntimeException("Member money creation failed", e);
        }
    }

    @Override
    public MoneyChangingRequest increaseMoneyRequestByEvent(IncreaseMoneyRequestCommand command) {
        try {
            // 기존 멤버 머니 조회
            MemberMoneyJpaEntity memberMoneyJpaEntity = getMemberMoneyPort.getMemberMoney(
                new MemberMoney.MembershipId(command.getTargetMembershipId())
            );
            
            if (memberMoneyJpaEntity == null) {
                throw new RuntimeException("Member money not found for membership: " + command.getTargetMembershipId());
            }
            
            String aggregateId = memberMoneyJpaEntity.getAggregateIdentifier();
            if (aggregateId == null || aggregateId.isEmpty()) {
                // 기존 데이터에 aggregate ID가 없으면 새로 생성
                aggregateId = "money-" + command.getTargetMembershipId();
                memberMoneyJpaEntity.setAggregateIdentifier(aggregateId);
            }
            
            // 이벤트 소싱 기반으로 머니 증가 요청 처리
            MoneyAggregate aggregate = getOrCreateMoneyAggregate(aggregateId, command.getTargetMembershipId());
            
            // 머니 증가 요청 이벤트 발생
            String taskId = UUID.randomUUID().toString();
            aggregate.requestMoneyIncrease(command.getAmount(), taskId);
            
            // 어그리게이트 저장 (이벤트 저장)
            moneyAggregateRepository.save(aggregate).join();
            
            // Saga 시작을 위한 태스크 생성 및 전송
            RechargingMoneyTask task = createTask(command, taskId);
            sendRechargingMoneyTaskPort.sendRechargingMoneyTaskPort(task);
            
            logger.info("Money increase request by event sent successfully: taskId={}, aggregateId={}", taskId, aggregateId);
            
            // 대기 중인 요청 생성 및 반환
            return createPendingMoneyChangingRequest(command, taskId);
            
        } catch (Exception e) {
            logger.error("Failed to process money increase request by event: {}", command.getTargetMembershipId(), e);
            throw new RuntimeException("Money increase request by event failed", e);
        }
    }
    
    /**
     * 머니 어그리게이트 생성 또는 조회 (이벤트 소싱 기반)
     */
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
    
    /**
     * 태스크 생성 (이벤트 소싱 기반)
     */
    private RechargingMoneyTask createTask(IncreaseMoneyRequestCommand command, String taskId) {
        List<SubTask> subTaskList = new ArrayList<>();
        
        subTaskList.add(SubTask.builder()
            .subTaskName("validMemberTask : 멤버십 유효성 검사")
            .membershipID(command.getTargetMembershipId())
            .taskType("membership")
            .status("ready")
            .build());
            
        subTaskList.add(SubTask.builder()
            .subTaskName("validBankingAccountTask : 뱅킹 계좌 유효성 검사")
            .membershipID(command.getTargetMembershipId())
            .taskType("banking")
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
