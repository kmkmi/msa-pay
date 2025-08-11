package com.msapay.common;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

/**
 * Saga 관리를 위한 공통 매니저
 * 각 서비스에서 독립적으로 사용할 수 있도록 Spring 의존성 제거
 */
public class SagaManager {

    private static final Logger logger = LoggerFactory.getLogger(SagaManager.class);

    /**
     * Increase Money Saga 시작
     * @param task 충전 태스크
     * @return 생성된 Saga ID
     */
    public String beginIncreaseMoneySaga(RechargingMoneyTask task) {
        try {
            String sagaId = UUID.randomUUID().toString();
            
            logger.info("Started IncreaseMoneySaga for task: {} with sagaId: {}", 
                task.getTaskID(), sagaId);
            
            // Saga 시작 로그 기록
            logSagaStart(sagaId, task);
            
            return sagaId;
            
        } catch (Exception e) {
            logger.error("Failed to begin IncreaseMoneySaga for task: {}", task.getTaskID(), e);
            throw new RuntimeException("Failed to begin saga", e);
        }
    }

    /**
     * Saga 시작 로그 기록
     */
    private void logSagaStart(String sagaId, RechargingMoneyTask task) {
        logger.info("Saga started - ID: {}, Task: {}, Amount: {}, Membership: {}", 
            sagaId, task.getTaskID(), task.getMoneyAmount(), task.getMembershipID());
    }

    /**
     * Saga 상태 업데이트 로그
     */
    public void updateSagaStatus(String sagaId, String status, String details) {
        logger.info("Saga status updated - ID: {}, Status: {}, Details: {}", 
            sagaId, status, details);
    }

    /**
     * Saga 완료 로그
     */
    public void completeSaga(String sagaId, boolean success, String result) {
        String status = success ? "COMPLETED" : "FAILED";
        logger.info("Saga completed - ID: {}, Status: {}, Result: {}", 
            sagaId, status, result);
    }
}
