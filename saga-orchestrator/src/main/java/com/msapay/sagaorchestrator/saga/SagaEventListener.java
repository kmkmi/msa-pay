package com.msapay.sagaorchestrator.saga;

import com.fasterxml.jackson.databind.JsonNode;
import com.msapay.sagaorchestrator.messaging.increaseMoneyEvent;
import com.msapay.sagaorchestrator.messaging.firmbankingEvent;
import com.msapay.sagaorchestrator.messaging.increaseMoneyStatus;
import com.msapay.sagaorchestrator.messaging.firmbankingStatus;
import com.msapay.sagaorchestrator.messaging.MoneyIncreaseEventProducer;
import com.msapay.sagaorchestrator.messaging.FirmbankingEventProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class SagaEventListener {

    private final IncreaseMoneySaga increaseMoneySaga;
    private final MoneyIncreaseEventProducer moneyIncreaseEventProducer;
    private final FirmbankingEventProducer firmbankingEventProducer;

    @EventListener
    public void handleIncreaseMoneyRequest(SagaEvent event) {
        if ("REQUEST_INCREASE_MONEY".equals(event.type())) {
            try {
                log.info("Processing REQUEST_INCREASE_MONEY event for saga: {}", event.aggregateId());
                
                // money-service가 구독할 토픽에 머니 증가 요청 이벤트 발행
                publishMoneyIncreaseRequestEvent(event);
                
            } catch (Exception e) {
                log.error("Failed to process REQUEST_INCREASE_MONEY event for saga: {}", event.aggregateId(), e);
            }
        }
    }

    @EventListener
    public void handleFirmbankingRequest(SagaEvent event) {
        if ("REQUEST_FIRMBANKING".equals(event.type())) {
            try {
                log.info("Processing REQUEST_FIRMBANKING event for saga: {}", event.aggregateId());
                
                // banking-service가 구독할 토픽에 펌뱅킹 요청 이벤트 발행
                publishFirmbankingRequestEvent(event);
                
            } catch (Exception e) {
                log.error("Failed to process REQUEST_FIRMBANKING event for saga: {}", event.aggregateId(), e);
            }
        }
    }

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

    private void publishMoneyIncreaseRequestEvent(SagaEvent event) {
        try {
            JsonNode payload = event.payload();
            String taskId = payload.get("taskId").asText();
            String membershipId = payload.get("membershipId").asText();
            int amount = payload.get("moneyAmount").asInt();
            
            log.info("Publishing money increase request event to Kafka - taskId: {}, membershipId: {}, amount: {}", 
                taskId, membershipId, amount);
            
            // Kafka Producer를 사용하여 money-service 토픽에 이벤트 발행
            moneyIncreaseEventProducer.sendMoneyIncreaseRequestEvent(event.aggregateId(), payload);
            
            log.info("Money increase request event published to Kafka for saga: {}", event.aggregateId());
            
        } catch (Exception e) {
            log.error("Failed to publish money increase request event", e);
            handleMoneyIncreaseResponse(event.aggregateId(), false, "Event publishing failed: " + e.getMessage());
        }
    }

    private void publishFirmbankingRequestEvent(SagaEvent event) {
        try {
            JsonNode payload = event.payload();
            String taskId = payload.get("taskId").asText();
            String membershipId = payload.get("membershipId").asText();
            int amount = payload.get("moneyAmount").asInt();
            
            log.info("Publishing firmbanking request event to Kafka - taskId: {}, membershipId: {}, amount: {}", 
                taskId, membershipId, amount);
            
            // Kafka Producer를 사용하여 banking-service 토픽에 이벤트 발행
            firmbankingEventProducer.sendFirmbankingRequestEvent(event.aggregateId(), payload);
            
            log.info("Firmbanking request event published to Kafka for saga: {}", event.aggregateId());
            
        } catch (Exception e) {
            log.error("Failed to publish firmbanking request event", e);
            handleFirmbankingResponse(event.aggregateId(), false, "Event publishing failed: " + e.getMessage());
        }
    }
}
