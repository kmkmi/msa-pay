package com.msapay.sagaorchestrator.saga;

import com.fasterxml.jackson.databind.JsonNode;
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
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import java.util.UUID;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class IncreaseMoneySaga implements Saga {

    private static final Logger logger = LoggerFactory.getLogger(IncreaseMoneySaga.class);

    private final ApplicationEventPublisher eventPublisher;
    private final TaskResultProducer taskResultProducer;
    private final ObjectMapper objectMapper;
    private final SagaStateService sagaStateService;
    private final com.msapay.common.SagaManager sagaManager;
    
    @Setter
    private SagaState sagaState;

    /**
     * Increase Money Saga 시작
     */
    public String beginIncreaseMoneySaga(RechargingMoneyTask task) {
        try {
            // 공통 Saga 매니저를 통한 기본 로깅
            String sagaId = sagaManager.beginIncreaseMoneySaga(task);
            
            logger.info("Started Advanced IncreaseMoneySaga for task: {} with sagaId: {}", task.getTaskID(), sagaId);
            
            // JPA 기반 Saga 상태 생성 및 저장
            ObjectNode payload = objectMapper.createObjectNode();
            payload.put("taskId", task.getTaskID());
            payload.put("membershipId", task.getMembershipID());
            payload.put("moneyAmount", task.getMoneyAmount());
            
            SagaState sagaState = new SagaState("INCREASE_MONEY", payload);
            sagaState.currentStep("INIT");
            sagaState = sagaStateService.saveSagaState(sagaState);
            
            // Saga 상태 설정 및 초기화
            this.sagaState = sagaState;
            init();
            
            logger.info("Saga state created and initialized: {}", sagaState.id());
            return sagaState.id().toString();
            
        } catch (Exception e) {
            logger.error("Failed to begin IncreaseMoneySaga for task: {}", task.getTaskID(), e);
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
            sagaState.currentStep("INCREASE_MONEY");
            
            // 전체 Saga 상태 재계산
            sagaState.advanceSagaStatus();
            
            // 상태 저장
            sagaState = sagaStateService.saveSagaState(sagaState);
            
            logger.info("IncreaseMoneySaga initialized for saga: {}", sagaState.id());
            
            // 다음 단계로 진행: 머니 증가 요청
            requestIncreaseMoney();
            
        } catch (Exception e) {
            logger.error("Failed to initialize IncreaseMoneySaga for saga: {}", sagaState.id(), e);
            sagaState.updateStepStatus("INIT", SagaStepStatus.FAILED);
            sagaState = sagaStateService.saveSagaState(sagaState);
        }
    }

    public void onIncreaseMoneyEvent(UUID eventId, increaseMoneyEvent event) {
        try {
            logger.info("Processing increase money event: {} for saga: {}", eventId, sagaState.id());
            
            if (event.getStatus() == com.msapay.sagaorchestrator.messaging.increaseMoneyStatus.COMPLETED) {
                // 1. 단계 상태 업데이트
                sagaState.updateStepStatus("INCREASE_MONEY", SagaStepStatus.SUCCEEDED);
                sagaState.currentStep("FIRMBANKING");
                
                // 2. 전체 Saga 상태 재계산
                sagaState.advanceSagaStatus();
                
                // 3. 상태 저장
                sagaState = sagaStateService.saveSagaState(sagaState);
                
                logger.info("Increase money step completed for saga: {}", sagaState.id());
                
                // 4. 다음 단계로 진행: 펌뱅킹 요청
                requestFirmbanking();
                
            } else if (event.getStatus() == com.msapay.sagaorchestrator.messaging.increaseMoneyStatus.FAILED) {
                // 1. 단계 상태 업데이트
                sagaState.updateStepStatus("INCREASE_MONEY", SagaStepStatus.FAILED);
                
                // 2. 전체 Saga 상태 재계산
                sagaState.advanceSagaStatus();
                
                // 3. 상태 저장
                sagaState = sagaStateService.saveSagaState(sagaState);
                
                logger.error("Increase money step failed for saga: {}", sagaState.id());
                
                // 4. 실패 시 보상 처리
                compensateIncreaseMoney();
            }
            
        } catch (Exception e) {
            logger.error("Failed to process increase money event for saga: {}", sagaState.id(), e);
            sagaState.updateStepStatus("INCREASE_MONEY", SagaStepStatus.FAILED);
            sagaState = sagaStateService.saveSagaState(sagaState);
        }
    }

    public void onFirmbankingEvent(UUID eventId, firmbankingEvent event) {
        try {
            logger.info("Processing firmbanking event: {} for saga: {}", eventId, sagaState.id());
            
            if (event.getStatus() == com.msapay.sagaorchestrator.messaging.firmbankingStatus.COMPLETED) {
                // 1. 단계 상태 업데이트
                sagaState.updateStepStatus("FIRMBANKING", SagaStepStatus.SUCCEEDED);
                
                // 2. 전체 Saga 상태 재계산
                sagaState.advanceSagaStatus();
                
                // 3. 상태 저장
                sagaState = sagaStateService.saveSagaState(sagaState);
                
                logger.info("Firmbanking step completed for saga: {}", sagaState.id());
                
                // 4. 모든 단계 완료, Saga 성공
                completeSaga();
                
            } else if (event.getStatus() == com.msapay.sagaorchestrator.messaging.firmbankingStatus.FAILED) {
                // 1. 단계 상태 업데이트
                sagaState.updateStepStatus("FIRMBANKING", SagaStepStatus.FAILED);
                
                // 2. 전체 Saga 상태 재계산
                sagaState.advanceSagaStatus();
                
                // 3. 상태 저장
                sagaState = sagaStateService.saveSagaState(sagaState);
                
                logger.error("Firmbanking step failed for saga: {}", sagaState.id());
                
                // 4. 실패 시 보상 처리
                compensateFirmbanking();
            }
            
        } catch (Exception e) {
            logger.error("Failed to process firmbanking event for saga: {}", sagaState.id(), e);
            sagaState.updateStepStatus("FIRMBANKING", SagaStepStatus.FAILED);
            sagaState = sagaStateService.saveSagaState(sagaState);
        }
    }
    
    private void requestIncreaseMoney() {
        try {
            // 머니 증가 요청 이벤트 발행
            SagaEvent event = new SagaEvent(sagaState.id(), "REQUEST_INCREASE_MONEY", 
                sagaState.payload());
            eventPublisher.publishEvent(event);
            
            sagaState.updateStepStatus("INCREASE_MONEY", SagaStepStatus.STARTED);
            
            // 상태 저장
            sagaState = sagaStateService.saveSagaState(sagaState);
            
            logger.info("Requested increase money for saga: {}", sagaState.id());
            
        } catch (Exception e) {
            logger.error("Failed to request increase money for saga: {}", sagaState.id(), e);
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
            
            sagaState.updateStepStatus("FIRMBANKING", SagaStepStatus.STARTED);
            
            // 상태 저장
            sagaState = sagaStateService.saveSagaState(sagaState);
            
            logger.info("Requested firmbanking for saga: {}", sagaState.id());
            
        } catch (Exception e) {
            logger.error("Failed to request firmbanking for saga: {}", sagaState.id(), e);
            sagaState.updateStepStatus("FIRMBANKING", SagaStepStatus.FAILED);
            sagaState = sagaStateService.saveSagaState(sagaState);
        }
    }
    
    private void completeSaga() {
        try {
            // Saga 완료 이벤트 발행
            SagaEvent event = new SagaEvent(sagaState.id(), "SAGA_COMPLETED", 
                sagaState.payload());
            eventPublisher.publishEvent(event);
            
            // TaskResultProducer를 통해 결과 전송
            taskResultProducer.sendTaskResult(sagaState.id().toString(), 
                Map.of("status", "SUCCESS", "sagaId", sagaState.id()));
            
            logger.info("Saga completed successfully: {}", sagaState.id());
            
        } catch (Exception e) {
            logger.error("Failed to complete saga: {}", sagaState.id(), e);
        }
    }
    
    private void compensateIncreaseMoney() {
        try {
            // 머니 증가 실패 시 보상 처리
            SagaEvent event = new SagaEvent(sagaState.id(), "COMPENSATE_INCREASE_MONEY", 
                sagaState.payload());
            eventPublisher.publishEvent(event);
            
            sagaState.updateStepStatus("INCREASE_MONEY", SagaStepStatus.COMPENSATED);
            logger.info("Compensated increase money for saga: {}", sagaState.id());
            
        } catch (Exception e) {
            logger.error("Failed to compensate increase money for saga: {}", sagaState.id(), e);
        }
    }
    
    private void compensateFirmbanking() {
        try {
            // 펌뱅킹 실패 시 보상 처리
            SagaEvent event = new SagaEvent(sagaState.id(), "COMPENSATE_FIRMBANKING", 
                sagaState.payload());
            eventPublisher.publishEvent(event);
            
            sagaState.updateStepStatus("FIRMBANKING", SagaStepStatus.COMPENSATED);
            logger.info("Compensated firmbanking for saga: {}", sagaState.id());
            
        } catch (Exception e) {
            logger.error("Failed to compensate firmbanking for saga: {}", sagaState.id(), e);
        }
    }
}
