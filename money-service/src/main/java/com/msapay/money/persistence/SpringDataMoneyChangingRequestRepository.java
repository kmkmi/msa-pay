package com.msapay.money.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SpringDataMoneyChangingRequestRepository extends JpaRepository<MoneyChangingRequestJpaEntity, Long> {
    
    /**
     * UUID로 MoneyChangingRequest를 찾습니다.
     * 
     * @param uuid 찾을 UUID
     * @return Optional<MoneyChangingRequestJpaEntity>
     */
    Optional<MoneyChangingRequestJpaEntity> findByUuid(String uuid);
}
