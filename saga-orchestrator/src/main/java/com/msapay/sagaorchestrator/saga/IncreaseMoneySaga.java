package com.msapay.sagaorchestrator.saga;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.msapay.common.RechargingMoneyTask;
import com.msapay.sagaorchestrator.framework.Saga;
import com.msapay.sagaorchestrator.framework.SagaState;
import com.msapay.sagaorchestrator.framework.SagaStepStatus;
import com.msapay.sagaorchestrator.TaskResultProducer;
import com.msapay.sagaorchestrator.messaging.increaseMoneyEvent;
import com.msapay.sagaorchestrator.messaging.firmbankingEvent;
import com.msapay.sagaorchestrator.service.SagaStateService;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;


import java.util.UUID;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
@Transactional(isolation = org.springframework.transaction.annotation.Isolation.READ_COMMITTED)
public class IncreaseMoneySaga implements Saga {

    private final ApplicationEventPublisher eventPublisher;
    private final TaskResultProducer taskResultProducer;
    private final ObjectMapper objectMapper;
    private final SagaStateService sagaStateService;
    
    @Setter
    private SagaState sagaState;

    @Transactional(isolation = org.springframework.transaction.annotation.Isolation.READ_COMMITTED)
    public String beginIncreaseMoneySaga(RechargingMoneyTask task) {
        try {
            log.info("Starting IncreaseMoneySaga for task: {}", task.getTaskID());
            
            // task에 이미 sagaId가 있는지 확인
            if (task.getSagaId() != null && !task.getSagaId().trim().isEmpty()) {
                log.info("Task already has sagaId: {}, attempting to find existing saga", task.getSagaId());
                
                // 기존 saga가 존재하는지 확인
                try {
                    UUID existingSagaId = UUID.fromString(task.getSagaId());
                    SagaState existingSaga = sagaStateService.findSagaStateByIdWithLock(existingSagaId);
                    if (existingSaga != null) {
                        log.info("Found existing saga: {} for task: {}", existingSagaId, task.getTaskID());
                        this.sagaState = existingSaga;
                        return existingSagaId.toString();
                    } else {
                        log.warn("SagaId exists but saga not found in database: {} for task: {}", 
                            task.getSagaId(), task.getTaskID());
                        // sagaId는 있지만 실제로는 존재하지 않는 경우, 새로 생성
                    }
                } catch (IllegalArgumentException e) {
                    log.warn("Invalid sagaId format: {} for task: {}", task.getSagaId(), task.getTaskID());
                    // 잘못된 형식의 sagaId인 경우, 새로 생성
                }
            }
            
            // JPA 기반 Saga 상태 생성 및 저장
            ObjectNode payload = objectMapper.createObjectNode();
            payload.put("taskId", task.getTaskID());
            payload.put("membershipId", task.getMembershipID());
            payload.put("moneyAmount", task.getMoneyAmount());
            
            SagaState sagaState = new SagaState("INCREASE_MONEY", payload);
            sagaState.currentStep("INIT");
            
            try {
                sagaState = sagaStateService.saveSagaState(sagaState);
            } catch (Exception e) {
                log.error("Failed to save new saga state for task: {}", task.getTaskID(), e);
                throw new RuntimeException("Failed to create saga", e);
            }
            
            // Saga 상태 설정 및 초기화
            this.sagaState = sagaState;
            
            // task에 sagaState.id 설정
            task.setSagaId(sagaState.id().toString());
            
            init();
            
            log.info("Saga state created and initialized: {} for task: {}", sagaState.id(), task.getTaskID());
            return sagaState.id().toString();
            
        } catch (Exception e) {
            log.error("Failed to begin IncreaseMoneySaga for task: {}", task.getTaskID(), e);
            throw new RuntimeException("Failed to begin saga", e);
        }
    }


    @Override
    public void init() {
        try {
            // Saga 초기화 시 이벤트 발행
            SagaEvent event = new SagaEvent(sagaState.id(), "SAGA_INIT", objectMapper.createObjectNode());
            eventPublisher.publishEvent(event);
            
            // 초기 단계 상태 설정
            sagaState.updateStepStatus("INIT", SagaStepStatus.SUCCEEDED);
            sagaState.currentStep("FIRMBANKING");
            
            // 펌뱅킹 단계 상태 설정
            sagaState.updateStepStatus("FIRMBANKING", SagaStepStatus.STARTED);
            
            // 전체 Saga 상태 재계산
            sagaState.advanceSagaStatus();
            
            // 상태 저장
            try {
                sagaState = sagaStateService.saveSagaState(sagaState);
            } catch (Exception e) {
                log.error("Failed to save saga state during init for saga: {}", sagaState.id(), e);
                // 저장 실패 시에도 계속 진행 (이미 메모리에 상태가 있음)
            }
            
            log.info("IncreaseMoneySaga initialized for saga: {}", sagaState.id());
            
            // 다음 단계로 진행: 펌뱅킹 요청
            requestFirmbanking();
            
        } catch (Exception e) {
            log.error("Failed to initialize IncreaseMoneySaga for saga: {}", sagaState.id(), e);
            try {
                sagaState.updateStepStatus("INIT", SagaStepStatus.FAILED);
                sagaState = sagaStateService.saveSagaState(sagaState);
            } catch (Exception saveException) {
                log.error("Failed to save failed status during init for saga: {}", sagaState.id(), saveException);
            }
        }
    }

    public void onIncreaseMoneyEvent(UUID eventId, increaseMoneyEvent event) {
        try {
            // sagaState가 설정되지 않은 경우 데이터베이스에서 비관적 락으로 조회
            if (sagaState == null) {
                sagaState = sagaStateService.findSagaStateByIdWithLock(event.getSagaId());
                if (sagaState == null) {
                    log.error("SagaState not found for saga: {}", event.getSagaId());
                    return;
                }
            }
            
            log.info("Processing increase money event: {} for saga: {}", eventId, sagaState.id());
            
            // 이미 처리된 단계인지 확인
            if (sagaState.getStepStatus().has("INCREASE_MONEY")) {
                String currentStatus = sagaState.getStepStatus().get("INCREASE_MONEY").asText();
                if ("SUCCEEDED".equals(currentStatus) || "FAILED".equals(currentStatus)) {
                    log.info("Increase money step already processed for saga: {} with status: {}", 
                        sagaState.id(), currentStatus);
                    return;
                }
            }
            
            // Saga가 이미 완료되었는지 확인
            if (sagaState.sagaStatus() == com.msapay.sagaorchestrator.framework.SagaStatus.COMPLETED) {
                log.info("Saga already completed for saga: {}, skipping processing", sagaState.id());
                return;
            }
            
            if (event.getStatus() == com.msapay.sagaorchestrator.messaging.increaseMoneyStatus.COMPLETED) {
                // 1. 단계 상태 업데이트
                sagaState.updateStepStatus("INCREASE_MONEY", SagaStepStatus.SUCCEEDED);
                log.info("Updated INCREASE_MONEY step status to SUCCEEDED for saga: {}", sagaState.id());
                
                // 2. 전체 Saga 상태 재계산
                log.info("Before advanceSagaStatus - Current saga status: {}, step statuses: {}", 
                    sagaState.sagaStatus(), sagaState.getStepStatus());
                sagaState.advanceSagaStatus();
                log.info("After advanceSagaStatus - New saga status: {}, step statuses: {}", 
                    sagaState.sagaStatus(), sagaState.getStepStatus());
                
                // 3. 상태 저장 (비관적 락 사용)
                sagaState = sagaStateService.saveSagaState(sagaState);
                
                log.info("Increase money step completed for saga: {}", sagaState.id());
                
                // 4. 모든 단계 완료, Saga 성공
                log.info("Calling completeSaga() for saga: {}", sagaState.id());
                completeSaga();
                
            } else if (event.getStatus() == com.msapay.sagaorchestrator.messaging.increaseMoneyStatus.FAILED) {
                // 1. 단계 상태 업데이트
                sagaState.updateStepStatus("INCREASE_MONEY", SagaStepStatus.FAILED);
                
                // 2. 전체 Saga 상태 재계산
                sagaState.advanceSagaStatus();
                
                // 3. 상태 저장 (비관적 락 사용)
                sagaState = sagaStateService.saveSagaState(sagaState);
                
                log.error("Increase money step failed for saga: {}", sagaState.id());
                
                // 4. 실패 시 보상 처리
                compensateIncreaseMoney();
            }
            
        } catch (Exception e) {
            log.error("Failed to process increase money event for saga: {}", event.getSagaId(), e);
            if (sagaState != null) {
                try {
                    sagaState.updateStepStatus("INCREASE_MONEY", SagaStepStatus.FAILED);
                    sagaState = sagaStateService.saveSagaState(sagaState);
                } catch (Exception saveException) {
                    log.error("Failed to save failed status for saga: {}", sagaState.id(), saveException);
                }
            }
        }
    }

    public void onFirmbankingEvent(UUID eventId, firmbankingEvent event) {
        try {
            // sagaState가 설정되지 않은 경우 데이터베이스에서 비관적 락으로 조회
            if (sagaState == null) {
                sagaState = sagaStateService.findSagaStateByIdWithLock(event.getSagaId());
                if (sagaState == null) {
                    log.error("SagaState not found for saga: {}", event.getSagaId());
                    return;
                }
            }
            
            log.info("Processing firmbanking event: {} for saga: {}", eventId, sagaState.id());
            
            // 이미 처리된 단계인지 확인
            if (sagaState.getStepStatus().has("FIRMBANKING")) {
                String currentStatus = sagaState.getStepStatus().get("FIRMBANKING").asText();
                if ("SUCCEEDED".equals(currentStatus) || "FAILED".equals(currentStatus)) {
                    log.info("Firmbanking step already processed for saga: {} with status: {}", 
                        sagaState.id(), currentStatus);
                    return;
                }
            }
            
            // Saga가 이미 완료되었는지 확인
            if (sagaState.sagaStatus() == com.msapay.sagaorchestrator.framework.SagaStatus.COMPLETED) {
                log.info("Saga already completed for saga: {}, skipping processing", sagaState.id());
                return;
            }
            
            if (event.getStatus() == com.msapay.sagaorchestrator.messaging.firmbankingStatus.COMPLETED) {
                // 1. 단계 상태 업데이트
                sagaState.updateStepStatus("FIRMBANKING", SagaStepStatus.SUCCEEDED);
                sagaState.currentStep("INCREASE_MONEY");
                
                // 2. 전체 Saga 상태 재계산
                sagaState.advanceSagaStatus();
                
                // 3. 상태 저장 (비관적 락 사용)
                sagaState = sagaStateService.saveSagaState(sagaState);
                
                log.info("Firmbanking step completed for saga: {}, proceeding to money increase", sagaState.id());
                
                // 4. 펌뱅킹 성공 시 머니 증가 요청 발행
                requestIncreaseMoney();
                
            } else if (event.getStatus() == com.msapay.sagaorchestrator.messaging.firmbankingStatus.FAILED) {
                // 1. 단계 상태 업데이트
                sagaState.updateStepStatus("FIRMBANKING", SagaStepStatus.FAILED);
                
                // 2. 전체 Saga 상태 재계산
                sagaState.advanceSagaStatus();
                
                // 3. 상태 저장 (비관적 락 사용)
                sagaState = sagaStateService.saveSagaState(sagaState);
                
                log.error("Firmbanking step failed for saga: {}", sagaState.id());
                
                // 4. 실패 시 보상 처리
                compensateFirmbanking();
            }
            
        } catch (Exception e) {
            log.error("Failed to process firmbanking event for saga: {}", event.getSagaId(), e);
            if (sagaState != null) {
                try {
                    sagaState.updateStepStatus("FIRMBANKING", SagaStepStatus.FAILED);
                    sagaState = sagaStateService.saveSagaState(sagaState);
                } catch (Exception saveException) {
                    log.error("Failed to save failed status for saga: {}", sagaState.id(), saveException);
                }
            }
        }
    }
    
    private void requestIncreaseMoney() {
        try {
            // 머니 증가 요청 이벤트 발행
            SagaEvent event = new SagaEvent(sagaState.id(), "REQUEST_INCREASE_MONEY", 
                sagaState.payload());
            eventPublisher.publishEvent(event);
            
            log.info("Requested increase money for saga: {}", sagaState.id());
            
        } catch (Exception e) {
            log.error("Failed to request increase money for saga: {}", sagaState.id(), e);
            sagaState.updateStepStatus("INCREASE_MONEY", SagaStepStatus.FAILED);
            sagaState = sagaStateService.saveSagaState(sagaState);
        }
    }
    
    private void requestFirmbanking() {
        try {
            // 펌뱅킹 요청 이벤트 발행
            SagaEvent event = new SagaEvent(sagaState.id(), "REQUEST_FIRMBANKING", 
                sagaState.payload());
            eventPublisher.publishEvent(event);
            
            log.info("Requested firmbanking for saga: {}", sagaState.id());
            
        } catch (Exception e) {
            log.error("Failed to request firmbanking for saga: {}", sagaState.id(), e);
            sagaState.updateStepStatus("FIRMBANKING", SagaStepStatus.FAILED);
            sagaState = sagaStateService.saveSagaState(sagaState);
        }
    }
    
    private void completeSaga() {
        try {
            log.info("Starting completeSaga() for saga: {} with status: {}", 
                sagaState.id(), sagaState.sagaStatus());
            
            // Saga 완료 이벤트 발행
            SagaEvent event = new SagaEvent(sagaState.id(), "SAGA_COMPLETED", 
                sagaState.payload());
            eventPublisher.publishEvent(event);
            log.info("Published SAGA_COMPLETED event for saga: {}", sagaState.id());
            
            // TaskResultProducer를 통해 결과 전송
            taskResultProducer.sendTaskResult(sagaState.id().toString(), 
                Map.of("status", "SUCCESS", "sagaId", sagaState.id()));
            log.info("Sent task result for saga: {}", sagaState.id());
            
            log.info("Saga completed successfully: {}", sagaState.id());
            
        } catch (Exception e) {
            log.error("Failed to complete saga: {}", sagaState.id(), e);
        }
    }
    
    private void compensateIncreaseMoney() {
        try {
            // 머니 증가 실패 시 보상 처리
            SagaEvent event = new SagaEvent(sagaState.id(), "COMPENSATE_INCREASE_MONEY", 
                sagaState.payload());
            eventPublisher.publishEvent(event);
            
            sagaState.updateStepStatus("INCREASE_MONEY", SagaStepStatus.COMPENSATED);
            log.info("Compensated increase money for saga: {}", sagaState.id());
            
        } catch (Exception e) {
            log.error("Failed to compensate increase money for saga: {}", sagaState.id(), e);
        }
    }
    
    private void compensateFirmbanking() {
        try {
            // 펌뱅킹 실패 시 보상 처리
            SagaEvent event = new SagaEvent(sagaState.id(), "COMPENSATE_FIRMBANKING", 
                sagaState.payload());
            eventPublisher.publishEvent(event);
            
            sagaState.updateStepStatus("FIRMBANKING", SagaStepStatus.COMPENSATED);
            log.info("Compensated firmbanking for saga: {}", sagaState.id());
            
        } catch (Exception e) {
            log.error("Failed to compensate firmbanking for saga: {}", sagaState.id(), e);
        }
    }
}
