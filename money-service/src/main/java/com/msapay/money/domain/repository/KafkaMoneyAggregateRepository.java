package com.msapay.money.domain.repository;

import com.msapay.money.domain.MoneyAggregate;
import com.msapay.money.domain.event.MoneyDomainEvent;
import com.msapay.money.domain.eventstore.EventStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Kafka 기반 머니 어그리게이트 저장소 구현
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class KafkaMoneyAggregateRepository implements MoneyAggregateRepository {
    
    private final EventStore eventStore;
    
    @Override
    public CompletableFuture<Void> save(MoneyAggregate aggregate) {
        return CompletableFuture.runAsync(() -> {
            try {
                List<MoneyDomainEvent> uncommittedEvents = aggregate.getUncommittedEvents();
                
                if (!uncommittedEvents.isEmpty()) {
                    // 이벤트 스토어에 이벤트들 저장
                    eventStore.saveEvents(
                        aggregate.getAggregateId(), 
                        uncommittedEvents, 
                        aggregate.getCurrentVersion() - uncommittedEvents.size()
                    ).join();
                    
                    // 커밋 완료 표시
                    aggregate.markEventsAsCommitted();
                    
                    log.info("MoneyAggregate saved successfully: {}, events: {}", 
                        aggregate.getAggregateId(), uncommittedEvents.size());
                }
                
            } catch (Exception e) {
                log.error("Failed to save MoneyAggregate: {}", aggregate.getAggregateId(), e);
                throw new RuntimeException("Failed to save aggregate", e);
            }
        });
    }
    
    @Override
    public CompletableFuture<MoneyAggregate> findById(String aggregateId) {
        return eventStore.getEvents(aggregateId)
            .thenApply(events -> {
                if (events.isEmpty()) {
                    return null;
                }
                return MoneyAggregate.fromEvents(aggregateId, events);
            });
    }
    
    @Override
    public CompletableFuture<MoneyAggregate> findByMembershipId(String membershipId) {
        // 멤버십 ID로 어그리게이트를 찾기 위해 이벤트 스토어에서 검색
        // 실제 구현에서는 멤버십 ID -> 어그리게이트 ID 매핑 테이블이 필요
        return CompletableFuture.completedFuture(null);
    }
    
    @Override
    public CompletableFuture<Boolean> exists(String aggregateId) {
        return eventStore.exists(aggregateId);
    }
}
