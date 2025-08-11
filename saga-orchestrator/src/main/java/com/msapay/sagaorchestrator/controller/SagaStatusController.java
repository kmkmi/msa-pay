package com.msapay.sagaorchestrator.controller;

import com.msapay.sagaorchestrator.framework.SagaState;
import com.msapay.sagaorchestrator.service.SagaStateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Saga 상태 조회를 위한 REST API 컨트롤러
 */
@Slf4j
@RestController
@RequestMapping("/api/saga")
@RequiredArgsConstructor
public class SagaStatusController {

    private final SagaStateService sagaStateService;

    /**
     * 특정 Saga의 상태 조회
     */
    @GetMapping("/{sagaId}/status")
    public ResponseEntity<SagaState> getSagaStatus(@PathVariable UUID sagaId) {
        try {
            log.info("Requesting saga status for sagaId: {}", sagaId);
            
            return sagaStateService.findSagaState(sagaId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
                
        } catch (Exception e) {
            log.error("Failed to get saga status for sagaId: {}", sagaId, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Saga 상태 요약 정보 조회
     */
    @GetMapping("/{sagaId}/summary")
    public ResponseEntity<SagaSummary> getSagaSummary(@PathVariable UUID sagaId) {
        try {
            log.info("Requesting saga summary for sagaId: {}", sagaId);
            
            return sagaStateService.findSagaState(sagaId)
                .map(sagaState -> {
                    SagaSummary summary = new SagaSummary(
                        sagaState.id(),
                        sagaState.type(),
                        sagaState.sagaStatus().name(),
                        sagaState.currentStep(),
                        sagaState.getCreatedAt().toString()
                    );
                    return ResponseEntity.ok(summary);
                })
                .orElse(ResponseEntity.notFound().build());
                
        } catch (Exception e) {
            log.error("Failed to get saga summary for sagaId: {}", sagaId, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Saga 상태 요약 정보 DTO
     */
    public static class SagaSummary {
        private final UUID sagaId;
        private final String type;
        private final String status;
        private final String currentStep;
        private final String createdAt;

        public SagaSummary(UUID sagaId, String type, String status, String currentStep, String createdAt) {
            this.sagaId = sagaId;
            this.type = type;
            this.status = status;
            this.currentStep = currentStep;
            this.createdAt = createdAt;
        }

        // Getters
        public UUID getSagaId() { return sagaId; }
        public String getType() { return type; }
        public String getStatus() { return status; }
        public String getCurrentStep() { return currentStep; }
        public String getCreatedAt() { return createdAt; }
    }
}
