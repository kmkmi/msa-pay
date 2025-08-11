package com.msapay.money.domain.eventstore;

import com.msapay.money.domain.event.MoneyDomainEvent;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * 이벤트 스토어 인터페이스
 */
public interface EventStore {
    
    /**
     * 이벤트 저장
     */
    CompletableFuture<Void> saveEvents(String aggregateId, List<MoneyDomainEvent> events, long expectedVersion);
    
    /**
     * 어그리게이트의 모든 이벤트 조회
     */
    CompletableFuture<List<MoneyDomainEvent>> getEvents(String aggregateId);
    
    /**
     * 특정 버전 이후의 이벤트 조회
     */
    CompletableFuture<List<MoneyDomainEvent>> getEventsAfterVersion(String aggregateId, long version);
    
    /**
     * 어그리게이트 존재 여부 확인
     */
    CompletableFuture<Boolean> exists(String aggregateId);
}
