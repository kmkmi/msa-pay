package com.msapay.money.domain.eventstore;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.msapay.money.domain.event.MoneyDomainEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Kafka 기반 이벤트 스토어 구현
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class KafkaEventStore implements EventStore {
    
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    
    // 메모리 기반 이벤트 저장소 (실제 운영에서는 Redis나 DB 사용 권장)
    private final ConcurrentHashMap<String, List<MoneyDomainEvent>> eventStore = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, AtomicLong> versionStore = new ConcurrentHashMap<>();
    
    private static final String MONEY_EVENTS_TOPIC = "money-domain-events";
    
    @Override
    public CompletableFuture<Void> saveEvents(String aggregateId, List<MoneyDomainEvent> events, long expectedVersion) {
        return CompletableFuture.runAsync(() -> {
            try {
                // 버전 검증
                AtomicLong currentVersion = versionStore.computeIfAbsent(aggregateId, k -> new AtomicLong(0));
                if (currentVersion.get() != expectedVersion) {
                    throw new RuntimeException("Optimistic concurrency control failed. Expected version: " + expectedVersion + ", but current version: " + currentVersion.get());
                }
                
                // 이벤트들을 Kafka로 전송
                for (MoneyDomainEvent event : events) {
                    sendEventToKafka(aggregateId, event);
                }
                
                // 로컬 이벤트 스토어에 저장
                eventStore.computeIfAbsent(aggregateId, k -> new java.util.ArrayList<>()).addAll(events);
                
                // 버전 업데이트
                currentVersion.addAndGet(events.size());
                
                log.info("Events saved successfully for aggregate: {}, version: {}", aggregateId, currentVersion.get());
                
            } catch (Exception e) {
                log.error("Failed to save events for aggregate: {}", aggregateId, e);
                throw new RuntimeException("Failed to save events", e);
            }
        });
    }
    
    @Override
    public CompletableFuture<List<MoneyDomainEvent>> getEvents(String aggregateId) {
        return CompletableFuture.completedFuture(
            eventStore.getOrDefault(aggregateId, new java.util.ArrayList<>())
        );
    }
    
    @Override
    public CompletableFuture<List<MoneyDomainEvent>> getEventsAfterVersion(String aggregateId, long version) {
        return getEvents(aggregateId).thenApply(events -> 
            events.stream()
                .filter(event -> event.getVersion() > version)
                .collect(java.util.stream.Collectors.toList())
        );
    }
    
    @Override
    public CompletableFuture<Boolean> exists(String aggregateId) {
        return CompletableFuture.completedFuture(eventStore.containsKey(aggregateId));
    }
    
    /**
     * 이벤트를 Kafka로 전송
     */
    private void sendEventToKafka(String aggregateId, MoneyDomainEvent event) {
        try {
            String eventJson = objectMapper.writeValueAsString(event);
            String key = aggregateId + "-" + event.getVersion();
            
            var future = kafkaTemplate.send(MONEY_EVENTS_TOPIC, key, eventJson);
            
            future.addCallback(
                result -> log.debug("Event sent to Kafka successfully: {} -> partition: {}, offset: {}", 
                    event.getEventId(), result.getRecordMetadata().partition(), result.getRecordMetadata().offset()),
                throwable -> log.error("Failed to send event to Kafka: {}", event.getEventId(), throwable)
            );
            
        } catch (Exception e) {
            log.error("Failed to serialize event: {}", event.getEventId(), e);
            throw new RuntimeException("Failed to serialize event", e);
        }
    }
}
