package com.msapay.banking.domain.repository;

import com.msapay.banking.domain.BankingAggregate;

import java.util.concurrent.CompletableFuture;

/**
 * 뱅킹 어그리게이트 저장소 인터페이스
 */
public interface BankingAggregateRepository {
    
    /**
     * 어그리게이트 저장
     */
    CompletableFuture<Void> save(BankingAggregate aggregate);
    
    /**
     * ID로 어그리게이트 조회
     */
    CompletableFuture<BankingAggregate> findById(String aggregateId);
    
    /**
     * 멤버십 ID로 어그리게이트 조회
     */
    CompletableFuture<BankingAggregate> findByMembershipId(String membershipId);
    
    /**
     * 어그리게이트 존재 여부 확인
     */
    CompletableFuture<Boolean> exists(String aggregateId);
}
