package com.msapay.sagaorchestrator.repository;

import com.msapay.sagaorchestrator.framework.SagaState;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface SagaStateRepository extends JpaRepository<SagaState, UUID> {
    // JpaRepository의 기본 메서드들만 사용:
    // - save(SagaState)
    // - findById(UUID)
}
