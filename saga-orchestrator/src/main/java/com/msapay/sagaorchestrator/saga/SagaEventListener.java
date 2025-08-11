package com.msapay.sagaorchestrator.saga;

import com.fasterxml.jackson.databind.JsonNode;
import com.msapay.sagaorchestrator.messaging.increaseMoneyEvent;
import com.msapay.sagaorchestrator.messaging.firmbankingEvent;
import com.msapay.sagaorchestrator.messaging.increaseMoneyStatus;
import com.msapay.sagaorchestrator.messaging.firmbankingStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Saga 이벤트를 처리하는 리스너
 * 외부 서비스로부터 받은 응답을 Saga에 전달하여 자동 진행
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SagaEventListener {

    private final IncreaseMoneySaga increaseMoneySaga;

    /**
     * 머니 증가 요청 이벤트 처리
     */
    @EventListener
    public void handleIncreaseMoneyRequest(SagaEvent event) {
        if ("REQUEST_INCREASE_MONEY".equals(event.type())) {
            try {
                log.info("Processing REQUEST_INCREASE_MONEY event for saga: {}", event.aggregateId());
                
                // 외부 서비스에 머니 증가 요청 전송
                // TODO: 실제 money-service API 호출 구현
                simulateMoneyIncreaseRequest(event);
                
            } catch (Exception e) {
                log.error("Failed to process REQUEST_INCREASE_MONEY event for saga: {}", event.aggregateId(), e);
            }
        }
    }

    /**
     * 펌뱅킹 요청 이벤트 처리
     */
    @EventListener
    public void handleFirmbankingRequest(SagaEvent event) {
        if ("REQUEST_FIRMBANKING".equals(event.type())) {
            try {
                log.info("Processing REQUEST_FIRMBANKING event for saga: {}", event.aggregateId());
                
                // 외부 서비스에 펌뱅킹 요청 전송
                // TODO: 실제 banking-service API 호출 구현
                simulateFirmbankingRequest(event);
                
            } catch (Exception e) {
                log.error("Failed to process REQUEST_FIRMBANKING event for saga: {}", event.aggregateId(), e);
            }
        }
    }

    /**
     * 머니 증가 응답 처리 (외부 서비스로부터 받은 응답)
     */
    public void handleMoneyIncreaseResponse(UUID sagaId, boolean success, String reason) {
        try {
            UUID eventId = UUID.randomUUID();
            
            if (success) {
                log.info("Money increase completed successfully for saga: {}", sagaId);
                increaseMoneySaga.onIncreaseMoneyEvent(
                    eventId,
                    new increaseMoneyEvent(increaseMoneyStatus.COMPLETED, sagaId, eventId)
                );
            } else {
                log.error("Money increase failed for saga: {} - reason: {}", sagaId, reason);
                increaseMoneySaga.onIncreaseMoneyEvent(
                    eventId,
                    new increaseMoneyEvent(increaseMoneyStatus.FAILED, sagaId, eventId)
                );
            }
            
        } catch (Exception e) {
            log.error("Failed to handle money increase response for saga: {}", sagaId, e);
        }
    }

    /**
     * 펌뱅킹 응답 처리 (외부 서비스로부터 받은 응답)
     */
    public void handleFirmbankingResponse(UUID sagaId, boolean success, String reason) {
        try {
            UUID eventId = UUID.randomUUID();
            
            if (success) {
                log.info("Firmbanking completed successfully for saga: {}", sagaId);
                increaseMoneySaga.onFirmbankingEvent(
                    eventId,
                    new firmbankingEvent(firmbankingStatus.COMPLETED, sagaId, eventId)
                );
            } else {
                log.error("Firmbanking failed for saga: {} - reason: {}", sagaId, reason);
                increaseMoneySaga.onFirmbankingEvent(
                    eventId,
                    new firmbankingEvent(firmbankingStatus.FAILED, sagaId, eventId)
                );
            }
            
        } catch (Exception e) {
            log.error("Failed to handle firmbanking response for saga: {}", sagaId, e);
        }
    }

    /**
     * 머니 증가 요청 시뮬레이션 (테스트용)
     */
    private void simulateMoneyIncreaseRequest(SagaEvent event) {
        try {
            JsonNode payload = event.payload();
            String taskId = payload.get("taskId").asText();
            String membershipId = payload.get("membershipId").asText();
            int amount = payload.get("moneyAmount").asInt();
            
            log.info("Simulating money increase request - taskId: {}, membershipId: {}, amount: {}", 
                taskId, membershipId, amount);
            
            // 비동기로 처리 시뮬레이션 (실제로는 외부 서비스 호출)
            new Thread(() -> {
                try {
                    Thread.sleep(2000); // 2초 대기
                    
                    // 성공 시뮬레이션 (90% 성공률)
                    boolean success = Math.random() > 0.1;
                    handleMoneyIncreaseResponse(event.aggregateId(), success, 
                        success ? "Success" : "Simulated failure");
                        
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }).start();
            
        } catch (Exception e) {
            log.error("Failed to simulate money increase request", e);
        }
    }

    /**
     * 펌뱅킹 요청 시뮬레이션 (테스트용)
     */
    private void simulateFirmbankingRequest(SagaEvent event) {
        try {
            JsonNode payload = event.payload();
            String taskId = payload.get("taskId").asText();
            String membershipId = payload.get("membershipId").asText();
            int amount = payload.get("moneyAmount").asInt();
            
            log.info("Simulating firmbanking request - taskId: {}, membershipId: {}, amount: {}", 
                taskId, membershipId, amount);
            
            // 비동기로 처리 시뮬레이션 (실제로는 외부 서비스 호출)
            new Thread(() -> {
                try {
                    Thread.sleep(3000); // 3초 대기
                    
                    // 성공 시뮬레이션 (95% 성공률)
                    boolean success = Math.random() > 0.05;
                    handleFirmbankingResponse(event.aggregateId(), success, 
                        success ? "Success" : "Simulated failure");
                        
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }).start();
            
        } catch (Exception e) {
            log.error("Failed to simulate firmbanking request", e);
        }
    }
}
