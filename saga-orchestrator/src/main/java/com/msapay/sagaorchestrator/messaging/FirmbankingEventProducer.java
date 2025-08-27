package com.msapay.sagaorchestrator.messaging;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class FirmbankingEventProducer {

    private final KafkaTemplate<String, String> kafkaTemplate;
    
    @Value("${kafka.topics.firmbanking-request}")
    private String topic;

    public void sendFirmbankingRequestEvent(UUID sagaId, JsonNode payload) {
        try {
            String message = createFirmbankingRequestMessage(sagaId, payload);
            String key = sagaId.toString();
            
            kafkaTemplate.send(topic, key, message);
            
            log.info("Firmbanking request event sent to topic: {} for saga: {}", topic, sagaId);
            
        } catch (Exception e) {
            log.error("Failed to send firmbanking request event for saga: {}", sagaId, e);
            throw new RuntimeException("Failed to send firmbanking request event", e);
        }
    }

    private String createFirmbankingRequestMessage(UUID sagaId, JsonNode payload) {
        return String.format(
            "{\"sagaId\":\"%s\",\"taskId\":\"%s\",\"membershipId\":\"%s\",\"amount\":%d,\"timestamp\":\"%s\"}",
            sagaId.toString(),
            payload.get("taskId").asText(),
            payload.get("membershipId").asText(),
            payload.get("moneyAmount").asInt(),
            System.currentTimeMillis()
        );
    }
}
