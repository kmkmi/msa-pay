package com.msapay.sagaorchestrator.service;

import com.msapay.sagaorchestrator.framework.SagaState;
import com.msapay.sagaorchestrator.repository.SagaStateRepository;
import com.msapay.common.UseCase;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@UseCase
@RequiredArgsConstructor
@Transactional
public class SagaStateService {

    private static final Logger logger = LoggerFactory.getLogger(SagaStateService.class);
    
    private final SagaStateRepository sagaStateRepository;

    /**
     * Saga 상태 저장
     */
    public SagaState saveSagaState(SagaState sagaState) {
        try {
            SagaState saved = sagaStateRepository.save(sagaState);
            logger.debug("Saved saga state: id={}, type={}, status={}", 
                saved.id(), saved.type(), saved.sagaStatus());
            return saved;
        } catch (Exception e) {
            logger.error("Failed to save saga state: id={}", sagaState.id(), e);
            throw new RuntimeException("Failed to save saga state", e);
        }
    }

    /**
     * Saga 상태 조회
     */
    public Optional<SagaState> findSagaState(UUID sagaId) {
        try {
            Optional<SagaState> sagaState = sagaStateRepository.findById(sagaId);
            if (sagaState.isPresent()) {
                logger.debug("Found saga state: id={}, type={}, status={}", 
                    sagaState.get().id(), sagaState.get().type(), sagaState.get().sagaStatus());
            } else {
                logger.debug("Saga state not found: id={}", sagaId);
            }
            return sagaState;
        } catch (Exception e) {
            logger.error("Failed to find saga state: id={}", sagaId, e);
            return Optional.empty();
        }
    }
}
