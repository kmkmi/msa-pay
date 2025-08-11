package com.msapay.money.domain.repository;

import com.msapay.money.domain.MoneyAggregate;

import java.util.concurrent.CompletableFuture;

/**
 * 머니 어그리게이트 저장소 인터페이스
 */
public interface MoneyAggregateRepository {
    
    /**
     * 어그리게이트 저장
     */
    CompletableFuture<Void> save(MoneyAggregate aggregate);
    
    /**
     * ID로 어그리게이트 조회
     */
    CompletableFuture<MoneyAggregate> findById(String aggregateId);
    
    /**
     * 멤버십 ID로 어그리게이트 조회
     */
    CompletableFuture<MoneyAggregate> findByMembershipId(String membershipId);
    
    /**
     * 어그리게이트 존재 여부 확인
     */
    CompletableFuture<Boolean> exists(String aggregateId);
}
