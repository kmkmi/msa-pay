package com.msapay.money.domain.event;

import com.msapay.common.outbox.OutboxEvent;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

// 머니 증가 요청 이벤트
@Getter
public class MoneyIncreaseRequestedEvent extends MoneyDomainEvent implements OutboxEvent<String, MoneyIncreaseRequestedEvent> {
    
    private final String membershipId;
    private final int amount;
    private final String taskId;
    
    public MoneyIncreaseRequestedEvent(String aggregateId, String membershipId, int amount, String taskId, long version) {
        super(aggregateId, "MoneyIncreaseRequested", version);
        this.membershipId = membershipId;
        this.amount = amount;
        this.taskId = taskId;
    }
    
    @Override
    public String getEventType() {
        return "MoneyIncreaseRequested";
    }
    
    // OutboxEvent 인터페이스 구현
    @Override
    public String aggregateId() {
        return getAggregateId();
    }
    
    @Override
    public String aggregateType() {
        return "MoneyAggregate";
    }
    
    @Override
    public String type() {
        return getEventType();
    }
    
    @Override
    public Instant timestamp() {
        return Instant.now();
    }
    
    @Override
    public MoneyIncreaseRequestedEvent payload() {
        return this;
    }
}
