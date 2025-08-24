package com.msapay.banking.inbound.kafka;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.msapay.banking.controller.command.GetRegisteredBankAccountCommand;
import com.msapay.banking.controller.command.RequestFirmbankingCommand;
import com.msapay.banking.domain.FirmbankingRequest;
import com.msapay.banking.domain.RegisteredBankAccount;
import com.msapay.banking.service.usecase.GetRegisteredBankAccountUseCase;
import com.msapay.banking.service.usecase.RequestFirmbankingUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class FirmbankingRequestConsumer {

    private final ObjectMapper objectMapper;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final RequestFirmbankingUseCase requestFirmbankingUseCase;
    private final GetRegisteredBankAccountUseCase getRegisteredBankAccountUseCase;
    
    @Value("${kafka.topics.firmbanking-result}")
    private String resultTopic;
    
    @Value("${kafka.topics.compensate-firmbanking-result}")
    private String compensateResultTopic;

    @KafkaListener(topics = "${kafka.topics.firmbanking-request}", groupId = "banking-service-group")
    public void consumeFirmbankingRequest(String message) {
        try {
            log.info("Received firmbanking request: {}", message);
            
            JsonNode event = objectMapper.readTree(message);
            String sagaId = event.get("sagaId").asText();
            String taskId = event.get("taskId").asText();
            String membershipId = event.get("membershipId").asText();
            int amount = event.get("amount").asInt();
            
            log.info("Processing firmbanking request - sagaId: {}, taskId: {}, membershipId: {}, amount: {}", 
                sagaId, taskId, membershipId, amount);
            
            // 펌뱅킹 처리
            boolean success = processFirmbanking(membershipId, amount);
            
            // 결과 이벤트 발행
            publishFirmbankingResult(sagaId, success, success ? "Success" : "Firmbanking failed");
            
        } catch (Exception e) {
            log.error("Failed to process firmbanking request: {}", message, e);
            
            // 에러 발생 시 실패 결과 발행
            try {
                JsonNode event = objectMapper.readTree(message);
                String sagaId = event.get("sagaId").asText();
                publishFirmbankingResult(sagaId, false, "Processing error: " + e.getMessage());
            } catch (Exception ex) {
                log.error("Failed to publish error result", ex);
            }
        }
    }

    // 보상 트랜잭션: 펌뱅킹 취소 처리
    @KafkaListener(topics = "${kafka.topics.compensate-firmbanking}", groupId = "banking-service-group")
    public void consumeCompensateFirmbanking(String message) {
        try {
            log.info("Received compensate firmbanking request: {}", message);
            
            JsonNode event = objectMapper.readTree(message);
            String sagaId = event.get("sagaId").asText();
            String taskId = event.get("taskId").asText();
            String membershipId = event.get("membershipId").asText();
            int amount = event.get("amount").asInt();
            
            log.info("Processing compensate firmbanking - sagaId: {}, taskId: {}, membershipId: {}, amount: {}", 
                sagaId, taskId, membershipId, amount);
            
            // 펌뱅킹 보상 처리 (취소)
            boolean success = processFirmbankingCompensation(membershipId, amount);
            
            // 보상 결과 이벤트 발행
            publishCompensateFirmbankingResult(sagaId, success, success ? "Compensation completed" : "Compensation failed");
            
        } catch (Exception e) {
            log.error("Failed to process compensate firmbanking request: {}", message, e);
            
            // 에러 발생 시 실패 결과 발행
            try {
                JsonNode event = objectMapper.readTree(message);
                String sagaId = event.get("sagaId").asText();
                publishCompensateFirmbankingResult(sagaId, false, "Compensation error: " + e.getMessage());
            } catch (Exception ex) {
                log.error("Failed to publish compensation error result", ex);
            }
        }
    }

    private boolean processFirmbanking(String membershipId, int amount) {
        try {

            RegisteredBankAccount bankAccount = getRegisteredBankAccountUseCase.getRegisteredBankAccount(new GetRegisteredBankAccountCommand(membershipId));

            RequestFirmbankingCommand command = RequestFirmbankingCommand.builder()
                    .toBankName("corpAccountBank")
                    .toBankAccountNumber("corpAccountNumber")
                    .fromBankName(bankAccount.getBankName())
                    .fromBankAccountNumber(bankAccount.getBankAccountNumber())
                    .moneyAmount(amount)
                    .build();

            FirmbankingRequest remittanceResult = requestFirmbankingUseCase.requestFirmbanking(command);
            log.info("Processing firmbanking for membership: {}, amount: {}", membershipId, amount);
            
            // 성공 시뮬레이션 (95% 성공률)
            boolean success = false;
            if (null != remittanceResult) {
                success = true;
            }
            
            if (success) {
                log.info("Firmbanking completed successfully for membership: {}", membershipId);
            } else {
                log.warn("Firmbanking failed for membership: {}", membershipId);
            }
            
            return success;
            
        } catch (Exception e) {
            log.error("Failed to process firmbanking for membership: {}", membershipId, e);
            return false;
        }
    }

    // 펌뱅킹 보상 처리 (취소)
    private boolean processFirmbankingCompensation(String membershipId, int amount) {
        try {
            log.info("Processing firmbanking compensation (cancellation) for membership: {}, amount: {}", membershipId, amount);
            
            // 1. 펌뱅킹 요청 상태 확인
            String firmbankingStatus = getFirmbankingStatus(membershipId, amount);
            log.info("Current firmbanking status for membership {}: {}", membershipId, firmbankingStatus);
            
            // 2. 펌뱅킹 취소 처리
            if ("COMPLETED".equals(firmbankingStatus) || "PROCESSING".equals(firmbankingStatus)) {
                boolean cancellationSuccess = cancelFirmbanking(membershipId, amount);
                
                if (cancellationSuccess) {
                    log.info("Firmbanking cancellation completed successfully for membership: {}", membershipId);
                    
                    // 3. 고객 계좌 잔액 복원 (펌뱅킹 취소로 인한)
                    boolean balanceRestoreSuccess = restoreCustomerBalance(membershipId, amount);
                    
                    if (balanceRestoreSuccess) {
                        log.info("Customer balance restored successfully for membership: {}", membershipId);
                        return true;
                    } else {
                        log.warn("Failed to restore customer balance for membership: {}", membershipId);
                        // 잔액 복원 실패해도 펌뱅킹 취소는 성공으로 처리
                        return true;
                    }
                } else {
                    log.error("Failed to cancel firmbanking for membership: {}", membershipId);
                    return false;
                }
            } else if ("CANCELLED".equals(firmbankingStatus) || "FAILED".equals(firmbankingStatus)) {
                log.info("Firmbanking already in cancelled/failed state for membership: {}", membershipId);
                return true; // 이미 취소된 상태
            } else {
                log.warn("Unknown firmbanking status for membership {}: {}", membershipId, firmbankingStatus);
                return false;
            }
            
        } catch (Exception e) {
            log.error("Failed to process firmbanking compensation for membership: {}", membershipId, e);
            return false;
        }
    }

    // 펌뱅킹 상태 조회 (실제 구현 필요)
    private String getFirmbankingStatus(String membershipId, int amount) {
        try {
            // TODO: 실제 데이터베이스에서 펌뱅킹 상태 조회
            log.info("Getting firmbanking status for membership: {}", membershipId);
            
            // 시뮬레이션: 80% 확률로 COMPLETED, 20% 확률로 PROCESSING
            double random = Math.random();
            if (random > 0.2) {
                return "COMPLETED";
            } else {
                return "PROCESSING";
            }
        } catch (Exception e) {
            log.error("Failed to get firmbanking status for membership: {}", membershipId, e);
            return "UNKNOWN";
        }
    }

    // 펌뱅킹 취소 (실제 구현 필요)
    private boolean cancelFirmbanking(String membershipId, int amount) {
        try {
            // TODO: 실제 외부 은행 API 호출하여 펌뱅킹 취소
            log.info("Cancelling firmbanking for membership: {}, amount: {}", membershipId, amount);
            
            // 시뮬레이션: 90% 성공률
            boolean success = Math.random() > 0.1;
            
            if (success) {
                log.info("Firmbanking cancelled successfully for membership: {}", membershipId);
            } else {
                log.warn("Firmbanking cancellation failed for membership: {}", membershipId);
            }
            
            return success;
            
        } catch (Exception e) {
            log.error("Failed to cancel firmbanking for membership: {}", membershipId, e);
            return false;
        }
    }

    // 고객 계좌 잔액 복원 (실제 구현 필요)
    private boolean restoreCustomerBalance(String membershipId, int amount) {
        try {
            RegisteredBankAccount bankAccount = getRegisteredBankAccountUseCase.getRegisteredBankAccount(new GetRegisteredBankAccountCommand(membershipId));

            RequestFirmbankingCommand command = RequestFirmbankingCommand.builder()
                    .toBankName(bankAccount.getBankName())
                    .toBankAccountNumber(bankAccount.getBankAccountNumber())
                    .fromBankName("corpAccountBank")
                    .fromBankAccountNumber("corpAccountNumber")
                    .moneyAmount(amount)
                    .build();

            FirmbankingRequest remittanceResult = requestFirmbankingUseCase.requestFirmbanking(command);
            log.info("Processing firmbanking for membership: {}, amount: {}", membershipId, amount);

            // 성공 시뮬레이션 (95% 성공률)
            boolean success = false;
            if (null != remittanceResult) {
                success = true;
            }

            if (success) {
                log.info("Customer balance restored successfully for membership: {}", membershipId);
            } else {
                log.warn("Customer balance restoration failed for membership: {}", membershipId);
            }
            
            return success;
            
        } catch (Exception e) {
            log.error("Failed to restore customer balance for membership: {}", membershipId, e);
            return false;
        }
    }

    private void publishFirmbankingResult(String sagaId, boolean success, String reason) {
        try {
            String resultMessage = createResultMessage(sagaId, success, reason);
            String key = sagaId;
            
            kafkaTemplate.send(resultTopic, key, resultMessage);
            
            log.info("Firmbanking result published - sagaId: {}, success: {}, reason: {}", 
                sagaId, success, reason);
            
        } catch (Exception e) {
            log.error("Failed to publish firmbanking result for saga: {}", sagaId, e);
        }
    }

    // 보상 결과 이벤트 발행
    private void publishCompensateFirmbankingResult(String sagaId, boolean success, String reason) {
        try {
            String resultMessage = createCompensateResultMessage(sagaId, success, reason);
            String key = sagaId;
            
            kafkaTemplate.send(compensateResultTopic, key, resultMessage);
            
            log.info("Compensate firmbanking result published - sagaId: {}, success: {}, reason: {}", 
                sagaId, success, reason);
            
        } catch (Exception e) {
            log.error("Failed to publish compensate firmbanking result for saga: {}", sagaId, e);
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
            "{\"sagaId\":\"%s\",\"success\":%s,\"reason\":\"%s\",\"type\":\"COMPENSATE_FIRMBANKING\",\"timestamp\":\"%s\"}",
            sagaId,
            success,
            reason,
            System.currentTimeMillis()
        );
    }
}
