package com.msapay.money.service;

import com.msapay.money.domain.MoneyAggregate;
import com.msapay.money.domain.event.MoneyDomainEvent;
import com.msapay.money.domain.eventstore.EventStore;
import com.msapay.money.domain.repository.MoneyAggregateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * 이벤트 리플레이 서비스 - 어그리게이트 상태 재구성
 */
@Slf4j
@Service
public class EventReplayService {
    
    private final EventStore eventStore;
    private final MoneyAggregateRepository moneyAggregateRepository;
    
    public EventReplayService(EventStore eventStore, MoneyAggregateRepository moneyAggregateRepository) {
        this.eventStore = eventStore;
        this.moneyAggregateRepository = moneyAggregateRepository;
    }
    
    /**
     * 특정 어그리게이트의 상태를 이벤트로부터 재구성
     */
    public CompletableFuture<MoneyAggregate> replayAggregate(String aggregateId) {
        return eventStore.getEvents(aggregateId)
            .thenApply(events -> {
                if (events.isEmpty()) {
                    log.warn("No events found for aggregate: {}", aggregateId);
                    return null;
                }
                
                log.info("Replaying {} events for aggregate: {}", events.size(), aggregateId);
                
                // 이벤트들을 순서대로 재생하여 어그리게이트 상태 복원
                MoneyAggregate aggregate = MoneyAggregate.fromEvents(aggregateId, events);
                
                log.info("Aggregate state rebuilt successfully: {} -> balance: {}, version: {}", 
                    aggregateId, aggregate.getBalance(), aggregate.getCurrentVersion());
                
                return aggregate;
            });
    }
    
    /**
     * 모든 어그리게이트의 상태를 이벤트로부터 재구성
     * 실제 구현에서는 어그리게이트 ID 목록을 관리해야 함
     */
    public CompletableFuture<Void> replayAllAggregates() {
        log.info("Starting replay of all aggregates...");
        
        // TODO: 실제 구현에서는 어그리게이트 ID 목록을 데이터베이스에서 조회
        // 현재는 예시로 하드코딩된 ID 사용
        List<String> aggregateIds = List.of("example-aggregate-1", "example-aggregate-2");
        
        List<CompletableFuture<MoneyAggregate>> replayFutures = aggregateIds.stream()
            .map(this::replayAggregate)
            .collect(java.util.stream.Collectors.toList());
        
        return CompletableFuture.allOf(replayFutures.toArray(new CompletableFuture[0]))
            .thenRun(() -> log.info("Completed replay of {} aggregates", aggregateIds.size()));
    }
    
    /**
     * 특정 시점 이후의 이벤트들을 재생하여 어그리게이트 상태 업데이트
     */
    public CompletableFuture<MoneyAggregate> replayEventsAfterVersion(String aggregateId, long version) {
        return eventStore.getEventsAfterVersion(aggregateId, version)
            .thenApply(events -> {
                if (events.isEmpty()) {
                    log.debug("No new events found for aggregate: {} after version: {}", aggregateId, version);
                    return null;
                }
                
                log.info("Replaying {} new events for aggregate: {} from version: {}", 
                    events.size(), aggregateId, version);
                
                // 기존 어그리게이트 조회
                return moneyAggregateRepository.findById(aggregateId)
                    .thenApply(existingAggregate -> {
                        if (existingAggregate == null) {
                            // 기존 어그리게이트가 없으면 새로 생성
                            return MoneyAggregate.fromEvents(aggregateId, events);
                        } else {
                            // 기존 어그리게이트에 새 이벤트들만 적용
                            for (MoneyDomainEvent event : events) {
                                existingAggregate.applyEvent(event);
                            }
                            return existingAggregate;
                        }
                    });
            })
            .thenCompose(future -> future);
    }
    
    /**
     * 정기적으로 모든 어그리게이트 상태를 이벤트로부터 재구성
     * 실제 운영에서는 필요에 따라 스케줄링 조정
     */
    @Scheduled(fixedRate = 300000) // 5분마다 실행
    public void scheduledReplay() {
        log.info("Starting scheduled event replay...");
        replayAllAggregates()
            .whenComplete((result, throwable) -> {
                if (throwable != null) {
                    log.error("Scheduled replay failed", throwable);
                } else {
                    log.info("Scheduled replay completed successfully");
                }
            });
    }
    
    /**
     * 특정 어그리게이트의 이벤트 히스토리 조회
     */
    public CompletableFuture<List<MoneyDomainEvent>> getEventHistory(String aggregateId) {
        return eventStore.getEvents(aggregateId);
    }
    
    /**
     * 어그리게이트의 특정 시점 상태 스냅샷 생성
     */
    public CompletableFuture<MoneyAggregate> createSnapshot(String aggregateId, long version) {
        return eventStore.getEventsAfterVersion(aggregateId, version)
            .thenApply(events -> {
                if (events.isEmpty()) {
                    return null;
                }
                
                // 특정 버전까지의 이벤트들로 스냅샷 생성
                List<MoneyDomainEvent> snapshotEvents = events.stream()
                    .filter(event -> event.getVersion() <= version)
                    .collect(java.util.stream.Collectors.toList());
                
                return MoneyAggregate.fromEvents(aggregateId, snapshotEvents);
            });
    }
}
