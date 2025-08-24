package com.msapay.money.inbound.kafka;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.msapay.money.domain.MemberMoney;
import com.msapay.money.domain.MoneyAggregate;
import com.msapay.money.persistence.MemberMoneyJpaEntity;
import com.msapay.money.service.IncreaseMoneyRequestService;
import com.msapay.money.service.port.IncreaseMoneyPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class MoneyIncreaseRequestConsumer {

    private final ObjectMapper objectMapper;
    private final IncreaseMoneyRequestService increaseMoneyRequestService;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final IncreaseMoneyPort increaseMoneyPort;
    
    @Value("${kafka.topics.money-increase-result}")
    private String resultTopic;
    
    @Value("${kafka.topics.compensate-increase-money-result}")
    private String compensateResultTopic;

    @KafkaListener(topics = "${kafka.topics.money-increase-request}", groupId = "money-service-group")
    public void consumeMoneyIncreaseRequest(String message) {
        try {
            log.info("Received money increase request: {}", message);
            
            JsonNode event = objectMapper.readTree(message);
            String sagaId = event.get("sagaId").asText();
            String taskId = event.get("taskId").asText();
            String membershipId = event.get("membershipId").asText();
            int amount = event.get("amount").asInt();
            
            log.info("Processing money increase request - sagaId: {}, taskId: {}, membershipId: {}, amount: {}", 
                sagaId, taskId, membershipId, amount);
            
            // 머니 증가 처리
            boolean success = processMoneyIncrease(membershipId, amount);
            
            // 결과 이벤트 발행
            publishMoneyIncreaseResult(sagaId, success, success ? "Success" : "Money increase failed");
            
        } catch (Exception e) {
            log.error("Failed to process money increase request: {}", message, e);
            
            // 에러 발생 시 실패 결과 발행
            try {
                JsonNode event = objectMapper.readTree(message);
                String sagaId = event.get("sagaId").asText();
                publishMoneyIncreaseResult(sagaId, false, "Processing error: " + e.getMessage());
            } catch (Exception ex) {
                log.error("Failed to publish error result", ex);
            }
        }
    }

    // 보상 트랜잭션: 머니 증가 취소 처리
    @KafkaListener(topics = "${kafka.topics.compensate-increase-money}", groupId = "money-service-group")
    public void consumeCompensateIncreaseMoney(String message) {
        try {
            log.info("Received compensate increase money request: {}", message);
            
            JsonNode event = objectMapper.readTree(message);
            String sagaId = event.get("sagaId").asText();
            String taskId = event.get("taskId").asText();
            String membershipId = event.get("membershipId").asText();
            int amount = event.get("amount").asInt();
            
            log.info("Processing compensate increase money - sagaId: {}, taskId: {}, membershipId: {}, amount: {}", 
                sagaId, taskId, membershipId, amount);
            
            // 머니 증가 보상 처리 (롤백)
            boolean success = processMoneyIncreaseCompensation(membershipId, amount);
            
            // 보상 결과 이벤트 발행
            publishCompensateIncreaseMoneyResult(sagaId, success, success ? "Compensation completed" : "Compensation failed");
            
        } catch (Exception e) {
            log.error("Failed to process compensate increase money request: {}", message, e);
            
            // 에러 발생 시 실패 결과 발행
            try {
                JsonNode event = objectMapper.readTree(message);
                String sagaId = event.get("sagaId").asText();
                publishCompensateIncreaseMoneyResult(sagaId, false, "Compensation error: " + e.getMessage());
            } catch (Exception ex) {
                log.error("Failed to publish compensation error result", ex);
            }
        }
    }

    private boolean processMoneyIncrease(String membershipId, int amount) {
        try {

            MemberMoneyJpaEntity memberMoneyJpaEntity = increaseMoneyPort.increaseMoney(
                    new MemberMoney.MembershipId(membershipId),
                    amount
            );
            log.info("Processing money increase for membership: {}, amount: {}", membershipId, amount);

            boolean success = false;
            if(null != memberMoneyJpaEntity){
                success = true;
            }
            
            if (success) {
                log.info("Money increase completed successfully for membership: {}", membershipId);
            } else {
                log.warn("Money increase failed for membership: {}", membershipId);
            }
            
            return success;
            
        } catch (Exception e) {
            log.error("Failed to process money increase for membership: {}", membershipId, e);
            return false;
        }
    }

    // 머니 증가 보상 처리 (롤백)
    private boolean processMoneyIncreaseCompensation(String membershipId, int amount) {
        try {
            log.info("Processing money increase compensation (rollback) for membership: {}, amount: {}", membershipId, amount);
            
            // 1. 현재 잔액 조회
            // TODO: 실제 잔액 조회 로직 구현
            int currentBalance = getCurrentBalance(membershipId);
            log.info("Current balance for membership {}: {}", membershipId, currentBalance);
            
            // 2. 머니 증가 취소 (잔액에서 차감)
            if (currentBalance >= amount) {
                int newBalance = currentBalance - amount;
                MemberMoneyJpaEntity memberMoneyJpaEntity = increaseMoneyPort.increaseMoney(
                        new MemberMoney.MembershipId(membershipId),
                        -1*amount
                );

                boolean rollbackSuccess = false;

                if(null != memberMoneyJpaEntity){
                    rollbackSuccess = true;
                }
                
                if (rollbackSuccess) {
                    log.info("Money increase compensation completed successfully for membership: {}. " +
                            "Balance changed from {} to {}", membershipId, currentBalance, newBalance);
                    return true;
                } else {
                    log.error("Failed to update balance during compensation for membership: {}", membershipId);
                    return false;
                }
            } else {
                log.warn("Insufficient balance for compensation. Current: {}, Required: {}", currentBalance, amount);
                // 잔액이 부족해도 보상은 성공으로 처리 (이미 차감된 상태일 수 있음)
                return true;
            }
            
        } catch (Exception e) {
            log.error("Failed to process money increase compensation for membership: {}", membershipId, e);
            return false;
        }
    }

    // 현재 잔액 조회 (실제 구현 필요)
    private int getCurrentBalance(String membershipId) {
        try {
            // TODO: 실제 데이터베이스에서 잔액 조회
            // 현재는 시뮬레이션으로 10000원 반환
            log.info("Getting current balance for membership: {}", membershipId);
            return 10000;
        } catch (Exception e) {
            log.error("Failed to get current balance for membership: {}", membershipId, e);
            return 0;
        }
    }

    private void publishMoneyIncreaseResult(String sagaId, boolean success, String reason) {
        try {
            String resultMessage = createResultMessage(sagaId, success, reason);
            String key = sagaId;
            
            kafkaTemplate.send(resultTopic, key, resultMessage);
            
            log.info("Money increase result published - sagaId: {}, success: {}, reason: {}", 
                sagaId, success, reason);
            
        } catch (Exception e) {
            log.error("Failed to publish money increase result for saga: {}", sagaId, e);
        }
    }

    // 보상 결과 이벤트 발행
    private void publishCompensateIncreaseMoneyResult(String sagaId, boolean success, String reason) {
        try {
            String resultMessage = createCompensateResultMessage(sagaId, success, reason);
            String key = sagaId;
            
            kafkaTemplate.send(compensateResultTopic, key, resultMessage);
            
            log.info("Compensate increase money result published - sagaId: {}, success: {}, reason: {}", 
                sagaId, success, reason);
            
        } catch (Exception e) {
            log.error("Failed to publish compensate increase money result for saga: {}", sagaId, e);
        }
    }

    private String createResultMessage(String sagaId, boolean success, String reason) {
        return String.format(
            "{\"sagaId\":\"%s\",\"success\":%s,\"reason\":\"%s\",\"timestamp\":\"%s\"}",
            sagaId,
            success,
            reason,
            System.currentTimeMillis()
        );
    }

    // 보상 결과 메시지 생성
    private String createCompensateResultMessage(String sagaId, boolean success, String reason) {
        return String.format(
            "{\"sagaId\":\"%s\",\"success\":%s,\"reason\":\"%s\",\"type\":\"COMPENSATE_INCREASE_MONEY\",\"timestamp\":\"%s\"}",
            sagaId,
            success,
            reason,
            System.currentTimeMillis()
        );
    }
}
