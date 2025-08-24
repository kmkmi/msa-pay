package com.msapay.sagaorchestrator.repository;

import com.msapay.sagaorchestrator.framework.SagaState;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface SagaStateRepository extends JpaRepository<SagaState, UUID> {
    
    // Native SQL을 사용하여 강력한 비관적 락 설정
    @Query(value = "SELECT * FROM sagastate WHERE id = :id FOR UPDATE SKIP LOCKED", nativeQuery = true)
    Optional<SagaState> findByIdWithLock(@Param("id") UUID id);
}
