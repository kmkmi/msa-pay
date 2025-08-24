package com.msapay.money.domain.eventstore;

import com.msapay.money.domain.event.MoneyDomainEvent;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * 이벤트 스토어 인터페이스
 */
public interface EventStore {
    
    CompletableFuture<Void> saveEvents(String aggregateId, List<MoneyDomainEvent> events, long expectedVersion);

    CompletableFuture<List<MoneyDomainEvent>> getEvents(String aggregateId);
    
    CompletableFuture<List<MoneyDomainEvent>> getEventsAfterVersion(String aggregateId, long version);

    CompletableFuture<Boolean> exists(String aggregateId);

    CompletableFuture<List<String>> getAllAggregateIds();
}
