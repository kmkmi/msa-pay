package com.msapay.money.domain.event;

import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 머니 증가 실패 이벤트
 */
@Getter
public class MoneyIncreaseFailedEvent extends MoneyDomainEvent {
    
    private final String membershipId;
    private final int amount;
    private final String taskId;
    private final String reason;
    
    public MoneyIncreaseFailedEvent(String aggregateId, String membershipId, int amount, String taskId, String reason, long version) {
        super(aggregateId, "MoneyIncreaseFailed", version);
        this.membershipId = membershipId;
        this.amount = amount;
        this.taskId = taskId;
        this.reason = reason;
    }
    
    @Override
    public String getEventType() {
        return "MoneyIncreaseFailed";
    }
}
