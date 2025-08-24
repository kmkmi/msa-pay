package com.msapay.sagaorchestrator.service;

import com.msapay.sagaorchestrator.framework.SagaState;
import com.msapay.sagaorchestrator.repository.SagaStateRepository;
import com.msapay.common.UseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@UseCase
@RequiredArgsConstructor
@Transactional(isolation = org.springframework.transaction.annotation.Isolation.READ_COMMITTED)
@Slf4j
public class SagaStateService {

    private final SagaStateRepository sagaStateRepository;
    private final JdbcTemplate jdbcTemplate;

    public SagaState saveSagaState(SagaState sagaState) {
        try {
            // INSERT ... ON DUPLICATE KEY UPDATE를 사용하여 자동으로 삽입 또는 업데이트
            insertNewSagaStateDirectly(sagaState);
            log.debug("Successfully saved saga state: id={}, type={}, status={}", 
                sagaState.id(), sagaState.type(), sagaState.sagaStatus());
            return sagaState;
            
        } catch (Exception e) {
            log.error("Failed to save saga state: id={}", sagaState.id(), e);
            throw new RuntimeException("Failed to save saga state", e);
        }
    }
    
    /**
     * JdbcTemplate을 사용하여 직접 SQL로 업데이트 (Hibernate 완전 우회)
     */
    private void updateExistingSagaStateDirectly(SagaState existing, SagaState newData) {
        try {
            // UUID를 문자열로 변환하여 직렬화 
            String sagaId = existing.id().toString();
            
            // JdbcTemplate을 사용하여 직접 SQL 실행
            String sql = "UPDATE sagastate SET current_step = ?, saga_status = ?, step_status = ? WHERE id = UUID_TO_BIN(?)";
            
            int updatedRows = jdbcTemplate.update(sql,
                newData.currentStep(),
                newData.sagaStatus().name(),
                newData.getStepStatus().toString(),
                sagaId  // UUID를 문자열로 전달
            );
            
            if (updatedRows == 0) {
                log.warn("No rows updated for saga: {}", sagaId);
            } else {
                log.debug("Successfully updated {} rows for saga: {}", updatedRows, sagaId);
            }
            
            // 메모리 객체도 업데이트
            existing.currentStep(newData.currentStep());
            existing.setStepStatus(newData.getStepStatus());
            existing.setSagaStatus(newData.sagaStatus());
            
            log.debug("Successfully updated saga state directly via JdbcTemplate: id={}", sagaId);
            
        } catch (Exception e) {
            log.error("Failed to update saga state directly via JdbcTemplate: id={}", existing.id(), e);
            throw new RuntimeException("Failed to update saga state directly", e);
        }
    }

    /**
     * JdbcTemplate을 사용하여 새로운 SagaState 직접 삽입 (Hibernate 완전 우회)
     */
    private void insertNewSagaStateDirectly(SagaState sagaState) {
        try {
            // UUID를 문자열로 변환하여 직렬화
            String sagaId = sagaState.id().toString();
            
            // INSERT ... ON DUPLICATE KEY UPDATE를 사용하여 중복 키 에러 방지
            String sql = "INSERT INTO sagastate (id, type, payload, current_step, step_status, saga_status, created_at) " +
                        "VALUES (UUID_TO_BIN(?), ?, ?, ?, ?, ?, ?) " +
                        "ON DUPLICATE KEY UPDATE " +
                        "type = VALUES(type), payload = VALUES(payload), current_step = VALUES(current_step), " +
                        "step_status = VALUES(step_status), saga_status = VALUES(saga_status), created_at = VALUES(created_at)";
            
            int affectedRows = jdbcTemplate.update(sql,
                sagaId,  // UUID를 문자열로 전달
                sagaState.type(),
                sagaState.payload().toString(),
                sagaState.currentStep(),
                sagaState.getStepStatus().toString(),
                sagaState.sagaStatus().name(),
                sagaState.getCreatedAt()
            );
            
            if (affectedRows == 1) {
                log.debug("Successfully inserted new saga state: id={}", sagaId);
            } else if (affectedRows == 2) {
                log.debug("Updated existing saga state due to duplicate key: id={}", sagaId);
            } else {
                log.warn("Unexpected affected rows: {} for saga: {}", affectedRows, sagaId);
            }
            
            log.debug("Successfully processed saga state via JdbcTemplate: id={}", sagaId);
            
        } catch (Exception e) {
            log.error("Failed to insert saga state directly via JdbcTemplate: id={}", sagaState.id(), e);
            throw new RuntimeException("Failed to insert saga state directly", e);
        }
    }

    public SagaState findSagaStateByIdWithLock(UUID id) {
        try {
            Optional<SagaState> sagaState = sagaStateRepository.findByIdWithLock(id);
            return sagaState.orElse(null);
        } catch (Exception e) {
            log.error("Failed to find saga state with lock: id={}", id, e);
            return null;
        }
    }
}
