package com.msapay.banking.domain.eventstore;

import com.msapay.banking.domain.event.BankingDomainEvent;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * 뱅킹 이벤트 스토어 인터페이스
 */
public interface BankingEventStore {
    
    /**
     * 이벤트 저장
     */
    CompletableFuture<Void> saveEvents(String aggregateId, List<BankingDomainEvent> events, long expectedVersion);
    
    /**
     * 어그리게이트의 모든 이벤트 조회
     */
    CompletableFuture<List<BankingDomainEvent>> getEvents(String aggregateId);
    
    /**
     * 특정 버전 이후의 이벤트 조회
     */
    CompletableFuture<List<BankingDomainEvent>> getEventsAfterVersion(String aggregateId, long version);
    
    /**
     * 어그리게이트 존재 여부 확인
     */
    CompletableFuture<Boolean> exists(String aggregateId);
}
