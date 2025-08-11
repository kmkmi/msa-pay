package com.msapay.banking.domain.repository;

import com.msapay.banking.domain.BankingAggregate;
import com.msapay.banking.domain.event.BankingDomainEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Kafka 기반 뱅킹 어그리게이트 저장소 구현
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class KafkaBankingAggregateRepository implements BankingAggregateRepository {
    
    // 임시 구현 - 실제로는 이벤트 스토어가 필요
    @Override
    public CompletableFuture<Void> save(BankingAggregate aggregate) {
        return CompletableFuture.runAsync(() -> {
            try {
                List<BankingDomainEvent> uncommittedEvents = aggregate.getUncommittedEvents();
                
                if (!uncommittedEvents.isEmpty()) {
                    // 임시로 로깅만 수행
                    log.info("BankingAggregate saved successfully: {}, events: {}", 
                        aggregate.getAggregateId(), uncommittedEvents.size());
                    
                    // 커밋 완료 표시
                    aggregate.markEventsAsCommitted();
                }
                
            } catch (Exception e) {
                log.error("Failed to save BankingAggregate: {}", aggregate.getAggregateId(), e);
                throw new RuntimeException("Failed to save aggregate", e);
            }
        });
    }
    
    @Override
    public CompletableFuture<BankingAggregate> findById(String aggregateId) {
        // 임시 구현 - null 반환
        return CompletableFuture.completedFuture(null);
    }
    
    @Override
    public CompletableFuture<BankingAggregate> findByMembershipId(String membershipId) {
        // 임시 구현 - null 반환
        return CompletableFuture.completedFuture(null);
    }
    
    @Override
    public CompletableFuture<Boolean> exists(String aggregateId) {
        // 임시 구현 - false 반환
        return CompletableFuture.completedFuture(false);
    }
}
