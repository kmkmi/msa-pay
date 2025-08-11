package com.msapay.money.domain.event;

import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 머니 증가 요청 이벤트
 */
@Getter
public class MoneyIncreaseRequestedEvent extends MoneyDomainEvent {
    
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
}
