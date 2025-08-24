package com.msapay.money.domain.repository;

import com.msapay.money.domain.MoneyAggregate;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * 머니 어그리게이트 저장소 인터페이스
 */
public interface MoneyAggregateRepository {

    CompletableFuture<Void> save(MoneyAggregate aggregate);

    CompletableFuture<MoneyAggregate> findById(String aggregateId);

    @Query("SELECT m.id FROM MoneyAggregate m")
    CompletableFuture<List<String>> findAllIds();

    CompletableFuture<MoneyAggregate> findByMembershipId(String membershipId);

    CompletableFuture<Boolean> exists(String aggregateId);
}
