package com.msapay.sagaorchestrator.messaging;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.msapay.sagaorchestrator.saga.SagaEventListener;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class SagaResultEventConsumer {

    private final ObjectMapper objectMapper;
    private final SagaEventListener sagaEventListener;

    @KafkaListener(topics = "${kafka.topics.firmbanking-result}", groupId = "saga-orchestrator-group")
    public void consumeFirmbankingResult(String message) {
        try {
            log.info("Received firmbanking result: {}", message);
            
            JsonNode result = objectMapper.readTree(message);
            String sagaId = result.get("sagaId").asText();
            boolean success = result.get("success").asBoolean();
            String reason = result.get("reason").asText();
            
            log.info("Processing firmbanking result - sagaId: {}, success: {}, reason: {}", 
                sagaId, success, reason);
            
            // SagaEventListener에 결과 전달
            sagaEventListener.handleFirmbankingResponse(UUID.fromString(sagaId), success, reason);
            
        } catch (Exception e) {
            log.error("Failed to process firmbanking result: {}", message, e);
        }
    }

    @KafkaListener(topics = "${kafka.topics.money-increase-result}", groupId = "saga-orchestrator-group")
    public void consumeMoneyIncreaseResult(String message) {
        try {
            log.info("Received money increase result: {}", message);
            
            JsonNode result = objectMapper.readTree(message);
            String sagaId = result.get("sagaId").asText();
            boolean success = result.get("success").asBoolean();
            String reason = result.get("reason").asText();
            
            log.info("Processing money increase result - sagaId: {}, success: {}, reason: {}", 
                sagaId, success, reason);
            
            // SagaEventListener에 결과 전달
            sagaEventListener.handleMoneyIncreaseResponse(UUID.fromString(sagaId), success, reason);
            
        } catch (Exception e) {
            log.error("Failed to process money increase result: {}", message, e);
        }
    }

    // 보상 트랜잭션 결과 처리: 머니 증가 보상 결과
    @KafkaListener(topics = "${kafka.topics.compensate-increase-money-result}", groupId = "saga-orchestrator-group")
    public void consumeCompensateIncreaseMoneyResult(String message) {
        try {
            log.info("Received compensate increase money result: {}", message);
            
            JsonNode result = objectMapper.readTree(message);
            String sagaId = result.get("sagaId").asText();
            boolean success = result.get("success").asBoolean();
            String reason = result.get("reason").asText();
            String type = result.has("type") ? result.get("type").asText() : "COMPENSATE_INCREASE_MONEY";
            
            log.info("Processing compensate increase money result - sagaId: {}, success: {}, reason: {}, type: {}", 
                sagaId, success, reason, type);
            
            // 보상 결과 처리 (현재는 로깅만, 필요시 추가 로직 구현)
            if (success) {
                log.info("Money increase compensation completed successfully for saga: {}", sagaId);
            } else {
                log.error("Money increase compensation failed for saga: {} - reason: {}", sagaId, reason);
                // 보상 실패 시 추가 처리 로직 구현 가능
            }
            
        } catch (Exception e) {
            log.error("Failed to process compensate increase money result: {}", message, e);
        }
    }

    // 보상 트랜잭션 결과 처리: 펌뱅킹 보상 결과
    @KafkaListener(topics = "${kafka.topics.compensate-firmbanking-result}", groupId = "saga-orchestrator-group")
    public void consumeCompensateFirmbankingResult(String message) {
        try {
            log.info("Received compensate firmbanking result: {}", message);
            
            JsonNode result = objectMapper.readTree(message);
            String sagaId = result.get("sagaId").asText();
            boolean success = result.get("success").asBoolean();
            String reason = result.get("reason").asText();
            String type = result.has("type") ? result.get("type").asText() : "COMPENSATE_FIRMBANKING";
            
            log.info("Processing compensate firmbanking result - sagaId: {}, success: {}, reason: {}, type: {}", 
                sagaId, success, reason, type);
            
            // 보상 결과 처리 (현재는 로깅만, 필요시 추가 로직 구현)
            if (success) {
                log.info("Firmbanking compensation completed successfully for saga: {}", sagaId);
            } else {
                log.error("Firmbanking compensation failed for saga: {} - reason: {}", sagaId, reason);
                // 보상 실패 시 추가 처리 로직 구현 가능
            }
            
        } catch (Exception e) {
            log.error("Failed to process compensate firmbanking result: {}", message, e);
        }
    }
}
