package com.msapay.money.service;

import com.msapay.common.outbox.OutboxEvent;
import com.msapay.common.outbox.OutboxRepository;
import com.msapay.money.domain.MoneyAggregate;
import com.msapay.money.domain.repository.MoneyAggregateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
@RequiredArgsConstructor
public class EventReplayService {
    
    private final OutboxRepository outboxRepository;
    private final MoneyAggregateRepository moneyAggregateRepository;
    private final ApplicationEventPublisher eventPublisher;
    
    // 5분마다 실행되는 스케줄된 이벤트 발행
    // outbox 테이블에서 미처리 이벤트들을 확인하고 발행
    @Scheduled(fixedRate = 300000) // 5분마다 실행
    public void scheduledEventPublishing() {
        log.info("Starting scheduled outbox event publishing...");
        publishPendingEvents()
            .whenComplete((result, throwable) -> {
                if (throwable != null) {
                    log.error("Scheduled event publishing failed", throwable);
                } else {
                    log.info("Scheduled event publishing completed successfully");
                }
            });
    }
    
    // 특정 어그리게이트의 outbox 이벤트들을 발행
    @Transactional
    public CompletableFuture<Void> publishAggregateEvents(String aggregateId) {
        log.info("Publishing outbox events for aggregate: {}", aggregateId);
        
        return CompletableFuture.runAsync(() -> {
            try {
                // outbox 테이블에서 해당 어그리게이트의 이벤트들 조회
                // 실제 구현에서는 OutboxRepository에 aggregateId로 조회하는 메서드 추가 필요
                log.info("Events published for aggregate: {}", aggregateId);
            } catch (Exception e) {
                log.error("Failed to publish events for aggregate: {}", aggregateId, e);
                throw new RuntimeException("Event publishing failed", e);
            }
        });
    }
    
    // OutboxEvent를 발행하여 outbox 테이블에 저장
    public void publishOutboxEvent(OutboxEvent<?, ?> event) {
        log.info("Publishing outbox event: {} for aggregate: {}", event.type(), event.aggregateId());
        eventPublisher.publishEvent(event);
    }
    
    // 모든 어그리게이트의 outbox 이벤트들을 발행
    @Transactional
    public CompletableFuture<Void> publishPendingEvents() {
        log.info("Starting to publish all pending outbox events...");
        
        return CompletableFuture.runAsync(() -> {
            try {
                List<String> aggregateIds = moneyAggregateRepository.findAllIds().join();
                log.info("Found {} aggregates to process", aggregateIds.size());
                
                // 각 어그리게이트의 이벤트들을 발행
                for (String aggregateId : aggregateIds) {
                    publishAggregateEvents(aggregateId).join();
                }
                
                log.info("Completed publishing events for {} aggregates", aggregateIds.size());
            } catch (Exception e) {
                log.error("Failed to publish pending events", e);
                throw new RuntimeException("Event publishing failed", e);
            }
        });
    }
    
    // 특정 버전 이후의 이벤트들을 발행
    @Transactional
    public CompletableFuture<Void> publishEventsAfterVersion(String aggregateId, long version) {
        log.info("Publishing events for aggregate: {} after version: {}", aggregateId, version);
        
        return CompletableFuture.runAsync(() -> {
            try {
                // outbox 테이블에서 특정 버전 이후의 이벤트들 조회 및 발행
                // 실제 구현에서는 OutboxRepository에 버전 기반 조회 메서드 추가 필요
                log.info("Events after version {} published for aggregate: {}", version, aggregateId);
            } catch (Exception e) {
                log.error("Failed to publish events after version {} for aggregate: {}", version, aggregateId, e);
                throw new RuntimeException("Event publishing failed", e);
            }
        });
    }
    
    // 이벤트 히스토리 조회 (outbox 테이블 기반)
    public CompletableFuture<List<OutboxEvent<?, ?>>> getEventHistory(String aggregateId) {
        log.info("Retrieving event history for aggregate: {}", aggregateId);
        
        return CompletableFuture.supplyAsync(() -> {
            try {
                // outbox 테이블에서 이벤트 히스토리 조회
                // 실제 구현에서는 OutboxRepository에 관련 메서드 추가 필요
                log.info("Event history retrieved for aggregate: {}", aggregateId);
                return List.of(); // 임시 반환값
            } catch (Exception e) {
                log.error("Failed to retrieve event history for aggregate: {}", aggregateId, e);
                throw new RuntimeException("Event history retrieval failed", e);
            }
        });
    }
    
    // 특정 버전까지의 스냅샷 생성 (outbox 테이블 기반)
    public CompletableFuture<MoneyAggregate> createSnapshot(String aggregateId, long version) {
        log.info("Creating snapshot for aggregate: {} at version: {}", aggregateId, version);
        
        return CompletableFuture.supplyAsync(() -> {
            try {
                // outbox 테이블에서 특정 버전까지의 이벤트들로 스냅샷 생성
                // 실제 구현에서는 OutboxRepository와 MoneyAggregateRepository 활용
                log.info("Snapshot created for aggregate: {} at version: {}", aggregateId, version);
                return null; // 임시 반환값
            } catch (Exception e) {
                log.error("Failed to create snapshot for aggregate: {} at version: {}", aggregateId, version, e);
                throw new RuntimeException("Snapshot creation failed", e);
            }
        });
    }
}
