package com.msapay.money.domain.event;

import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 머니 증가 완료 이벤트
 */
@Getter
public class MoneyIncreaseCompletedEvent extends MoneyDomainEvent {
    
    private final String membershipId;
    private final int amount;
    private final String taskId;
    private final int newBalance;
    
    public MoneyIncreaseCompletedEvent(String aggregateId, String membershipId, int amount, String taskId, int newBalance, long version) {
        super(aggregateId, "MoneyIncreaseCompleted", version);
        this.membershipId = membershipId;
        this.amount = amount;
        this.taskId = taskId;
        this.newBalance = newBalance;
    }
    
    @Override
    public String getEventType() {
        return "MoneyIncreaseCompleted";
    }
}
