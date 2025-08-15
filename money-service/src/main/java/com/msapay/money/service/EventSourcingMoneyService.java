package com.msapay.money.service;

import com.msapay.common.CountDownLatchManager;
import com.msapay.common.RechargingMoneyTask;
import com.msapay.common.SubTask;
import com.msapay.money.domain.MoneyAggregate;
import com.msapay.money.domain.repository.MoneyAggregateRepository;
import com.msapay.money.persistence.MemberMoneyJpaEntity;
import com.msapay.money.persistence.MoneyChangingRequestMapper;
import com.msapay.money.service.port.*;
import com.msapay.money.controller.request.MoneyChangingRequest;
import com.msapay.money.controller.command.IncreaseMoneyRequestCommand;
import com.msapay.money.service.usecase.IncreaseMoneyRequestUseCase;
import com.msapay.common.SagaManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * 이벤트 소싱 기반 머니 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class EventSourcingMoneyService implements IncreaseMoneyRequestUseCase {
    
    private final CountDownLatchManager countDownLatchManager;
    private final SendRechargingMoneyTaskPort sendRechargingMoneyTaskPort;
    private final GetMembershipPort membershipPort;
    private final IncreaseMoneyPort increaseMoneyPort;
    private final MoneyChangingRequestMapper mapper;
    private final SagaManager sagaManager;
    private final MoneyAggregateRepository moneyAggregateRepository;
    
    @Override
    public MoneyChangingRequest increaseMoneyRequest(IncreaseMoneyRequestCommand command) {
        // 동기 처리 - 기존 로직 유지
        return processIncreaseMoneyRequest(command, false);
    }
    
    @Override
    public MoneyChangingRequest increaseMoneyRequestAsync(IncreaseMoneyRequestCommand command) {
        // 비동기 처리 - 이벤트 소싱 적용
        return processIncreaseMoneyRequestAsync(command);
    }
    
    @Override
    public MoneyChangingRequest increaseMoneyRequestByEvent(IncreaseMoneyRequestCommand command) {
        // 이벤트 기반 머니 증가 요청 처리
        return processIncreaseMoneyRequest(command, true);
    }
    
    /**
     * 동기 머니 증가 요청 처리
     */
    private MoneyChangingRequest processIncreaseMoneyRequest(IncreaseMoneyRequestCommand command, boolean isAsync) {
        try {
            // 1. 고객 정보 유효성 검사
            membershipPort.getMembership(command.getTargetMembershipId());
            
            // 2. 머니 어그리게이트 생성 또는 조회
            String aggregateId = "money-" + command.getTargetMembershipId();
            MoneyAggregate aggregate = getOrCreateMoneyAggregate(aggregateId, command.getTargetMembershipId());
            
            // 3. 머니 증가 요청 이벤트 발생
            String taskId = isAsync ? UUID.randomUUID().toString() : "sync-" + UUID.randomUUID();
            aggregate.requestMoneyIncrease(command.getAmount(), taskId);
            
            // 4. 어그리게이트 저장 (이벤트 저장)
            moneyAggregateRepository.save(aggregate).join();
            
            // 5. 머니 증가 처리
            MemberMoneyJpaEntity memberMoneyJpaEntity = increaseMoneyPort.increaseMoney(
                new com.msapay.money.domain.MemberMoney.MembershipId(command.getTargetMembershipId()),
                command.getAmount()
            );
            
            if (memberMoneyJpaEntity != null) {
                // 6. 머니 증가 완료 이벤트 발생
                aggregate.completeMoneyIncrease(command.getAmount(), taskId);
                moneyAggregateRepository.save(aggregate).join();
                
                // 7. MoneyChangingRequest 생성 및 반환
                return createMoneyChangingRequest(command, taskId, 1); // 1: 성공
            } else {
                // 8. 머니 증가 실패 이벤트 발생
                aggregate.failMoneyIncrease(command.getAmount(), taskId, "Money increase operation failed");
                moneyAggregateRepository.save(aggregate).join();
                
                return createMoneyChangingRequest(command, taskId, 2); // 2: 실패
            }
            
        } catch (Exception e) {
            log.error("Failed to process money increase request: {}", command.getTargetMembershipId(), e);
            throw new RuntimeException("Money increase request failed", e);
        }
    }
    
    /**
     * 비동기 머니 증가 요청 처리 (이벤트 소싱 적용)
     */
    private MoneyChangingRequest processIncreaseMoneyRequestAsync(IncreaseMoneyRequestCommand command) {
        try {
            // 1. 머니 어그리게이트 생성 또는 조회
            String aggregateId = "money-" + command.getTargetMembershipId();
            MoneyAggregate aggregate = getOrCreateMoneyAggregate(aggregateId, command.getTargetMembershipId());
            
            // 2. 머니 증가 요청 이벤트 발생
            String taskId = UUID.randomUUID().toString();
            aggregate.requestMoneyIncrease(command.getAmount(), taskId);
            
            // 3. 어그리게이트 저장 (이벤트 저장)
            moneyAggregateRepository.save(aggregate).join();
            
            // 4. 태스크 생성 및 Kafka 전송
            RechargingMoneyTask task = createTask(command, taskId);
            sendRechargingMoneyTaskPort.sendRechargingMoneyTaskPort(task);
            log.info("Task sent to Kafka: {}", taskId);
            
            // 5. 즉시 응답 - 대기 중인 상태
            MoneyChangingRequest pendingRequest = createMoneyChangingRequest(command, taskId, 0); // 0: 대기 중
            
            // 6. 비동기로 결과 처리 시작
            processTaskResultAsync(aggregate, task, command);
            
            return pendingRequest;
            
        } catch (Exception e) {
            log.error("Failed to process async money increase request: {}", command.getTargetMembershipId(), e);
            throw new RuntimeException("Async money increase request failed", e);
        }
    }
    
    /**
     * 머니 어그리게이트 생성 또는 조회
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
     * 태스크 생성
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
    
    /**
     * MoneyChangingRequest 생성
     */
    private MoneyChangingRequest createMoneyChangingRequest(IncreaseMoneyRequestCommand command, String taskId, int status) {
        return mapper.mapToDomainEntity(increaseMoneyPort.createMoneyChangingRequest(
            new MoneyChangingRequest.TargetMembershipId(command.getTargetMembershipId()),
            new MoneyChangingRequest.MoneyChangingType(1),
            new MoneyChangingRequest.ChangingMoneyAmount(command.getAmount()),
            new MoneyChangingRequest.MoneyChangingStatus(status),
            new MoneyChangingRequest.Uuid(taskId)
        ));
    }
    
    /**
     * 비동기로 태스크 결과 처리
     */
    private void processTaskResultAsync(MoneyAggregate aggregate, RechargingMoneyTask task, IncreaseMoneyRequestCommand command) {
        CompletableFuture.runAsync(() -> {
            try {
                log.info("Starting async task result processing for task: {}", task.getTaskID());
                
                // CountDownLatch 등록
                countDownLatchManager.addCountDownLatch(task.getTaskID());
                
                // 태스크 결과 대기
                String result = waitForTaskResult(task.getTaskID());
                
                // 결과 처리
                processTaskResult(result, aggregate, task, command);
                
            } catch (Exception e) {
                log.error("Failed to process async task result for task: {}", task.getTaskID(), e);
                handleTaskProcessingError(aggregate, task, command, e);
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
    private void processTaskResult(String result, MoneyAggregate aggregate, RechargingMoneyTask task, IncreaseMoneyRequestCommand command) {
        if (result.equals("success")) {
            // 비동기 태스크 성공 시 Saga 시작
            String sagaId = sagaManager.beginIncreaseMoneySaga(task);
            log.info("Increase Money Saga started with ID: {} for task: {}", sagaId, task.getTaskID());
            
            // Saga 상태 업데이트
            sagaManager.updateSagaStatus(sagaId, "SUCCESS", "All subtasks completed successfully");
            
            try {
                // 머니 증가 처리
                MemberMoneyJpaEntity memberMoneyJpaEntity = increaseMoneyPort.increaseMoney(
                    new com.msapay.money.domain.MemberMoney.MembershipId(command.getTargetMembershipId()),
                    command.getAmount()
                );
                
                if (memberMoneyJpaEntity != null) {
                    // 머니 증가 완료 이벤트 발생
                    aggregate.completeMoneyIncrease(command.getAmount(), task.getTaskID());
                    moneyAggregateRepository.save(aggregate).join();
                    
                    // Saga 완료
                    sagaManager.completeSaga(sagaId, true, "Money increased successfully");
                    
                    // MoneyChangingRequest 상태 업데이트
                    updateMoneyChangingRequestStatus(task.getTaskID(), 1); // 1: 성공
                    
                    log.info("Money increase completed successfully for task: {}", task.getTaskID());
                } else {
                    // 머니 증가 실패 이벤트 발생
                    aggregate.failMoneyIncrease(command.getAmount(), task.getTaskID(), "Money increase operation failed");
                    moneyAggregateRepository.save(aggregate).join();
                    
                    // Saga 실패 처리
                    sagaManager.completeSaga(sagaId, false, "Money increase failed");
                    updateMoneyChangingRequestStatus(task.getTaskID(), 2); // 2: 실패
                    
                    log.error("Money increase failed for task: {}", task.getTaskID());
                }
                
            } catch (Exception e) {
                // 머니 증가 실패 이벤트 발생
                aggregate.failMoneyIncrease(command.getAmount(), task.getTaskID(), "Exception: " + e.getMessage());
                moneyAggregateRepository.save(aggregate).join();
                
                // Saga 실패 처리
                sagaManager.completeSaga(sagaId, false, "Exception: " + e.getMessage());
                updateMoneyChangingRequestStatus(task.getTaskID(), 2); // 2: 실패
                
                log.error("Money increase failed with exception for task: {}", task.getTaskID(), e);
            }
            
        } else {
            // 비동기 태스크 실패 시
            log.warn("Async task failed for task: {}, result: {}", task.getTaskID(), result);
            
            // 머니 증가 실패 이벤트 발생
            aggregate.failMoneyIncrease(command.getAmount(), task.getTaskID(), "Async task failed: " + result);
            moneyAggregateRepository.save(aggregate).join();
            
            updateMoneyChangingRequestStatus(task.getTaskID(), 2); // 2: 실패
        }
    }
    
    /**
     * 태스크 처리 에러 핸들링
     */
    private void handleTaskProcessingError(MoneyAggregate aggregate, RechargingMoneyTask task, IncreaseMoneyRequestCommand command, Exception e) {
        log.error("Task processing error for task: {}", task.getTaskID(), e);
        
        // 머니 증가 실패 이벤트 발생
        aggregate.failMoneyIncrease(command.getAmount(), task.getTaskID(), "Task processing error: " + e.getMessage());
        moneyAggregateRepository.save(aggregate).join();
        
        updateMoneyChangingRequestStatus(task.getTaskID(), 2); // 2: 실패
    }
    
    /**
     * MoneyChangingRequest 상태 업데이트
     */
    private void updateMoneyChangingRequestStatus(String taskId, int status) {
        try {
            // TODO: MoneyChangingRequest 상태 업데이트 로직 구현
            log.info("Updating MoneyChangingRequest status for task: {} to status: {}", taskId, status);
        } catch (Exception e) {
            log.error("Failed to update MoneyChangingRequest status for task: {}", taskId, e);
        }
    }
    
    @Override
    public List<com.msapay.money.domain.MemberMoney> findMemberMoneyListByMembershipIds(
            com.msapay.money.controller.command.FindMemberMoneyListByMembershipIdsCommand command) {
        // TODO: 이벤트 소싱 기반으로 구현
        throw new UnsupportedOperationException("Not implemented yet");
    }
}
