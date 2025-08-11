package com.msapay.money.domain;

import com.msapay.money.domain.event.MoneyDomainEvent;
import com.msapay.money.domain.event.MoneyIncreaseCompletedEvent;
import com.msapay.money.domain.event.MoneyIncreaseFailedEvent;
import com.msapay.money.domain.event.MoneyIncreaseRequestedEvent;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * 머니 어그리게이트 루트 - 이벤트 소싱 패턴 구현
 */
@Getter
@NoArgsConstructor
public class MoneyAggregate {
    
    private String aggregateId;
    private String membershipId;
    private int balance;
    private long version;
    private List<MoneyDomainEvent> uncommittedEvents = new ArrayList<>();
    
    public MoneyAggregate(String aggregateId, String membershipId) {
        this.aggregateId = aggregateId;
        this.membershipId = membershipId;
        this.balance = 0;
        this.version = 0;
    }
    
    /**
     * 이벤트 스토어에서 이벤트들을 재생하여 어그리게이트 상태 복원
     */
    public static MoneyAggregate fromEvents(String aggregateId, List<MoneyDomainEvent> events) {
        MoneyAggregate aggregate = new MoneyAggregate(aggregateId, "");
        
        for (MoneyDomainEvent event : events) {
            aggregate.applyEvent(event);
        }
        
        return aggregate;
    }
    
    /**
     * 멤버 머니 생성 처리
     */
    public void createMemberMoney(String membershipId) {
        // 멤버 머니 생성 이벤트는 상태를 변경하지 않음 (로깅 목적)
        // 실제 상태 변경은 머니 증가/감소 이벤트에서 처리
        version++;
    }
    
    /**
     * 머니 증가 요청 처리
     */
    public void requestMoneyIncrease(int amount, String taskId) {
        if (amount <= 0) {
            throw new IllegalArgumentException("Amount must be positive");
        }
        
        MoneyIncreaseRequestedEvent event = new MoneyIncreaseRequestedEvent(
            aggregateId, membershipId, amount, taskId, version + 1
        );
        
        applyEvent(event);
        uncommittedEvents.add(event);
    }
    
    /**
     * 머니 증가 완료 처리
     */
    public void completeMoneyIncrease(int amount, String taskId) {
        int newBalance = balance + amount;
        
        MoneyIncreaseCompletedEvent event = new MoneyIncreaseCompletedEvent(
            aggregateId, membershipId, amount, taskId, newBalance, version + 1
        );
        
        applyEvent(event);
        uncommittedEvents.add(event);
    }
    
    /**
     * 머니 증가 실패 처리
     */
    public void failMoneyIncrease(int amount, String taskId, String reason) {
        MoneyIncreaseFailedEvent event = new MoneyIncreaseFailedEvent(
            aggregateId, membershipId, amount, taskId, reason, version + 1
        );
        
        applyEvent(event);
        uncommittedEvents.add(event);
    }
    
    /**
     * 이벤트 적용하여 상태 변경
     */
    public void applyEvent(MoneyDomainEvent event) {
        if (event instanceof MoneyIncreaseRequestedEvent) {
            // 요청 이벤트는 상태를 변경하지 않음 (로깅 목적)
            version = event.getVersion();
        } else if (event instanceof MoneyIncreaseCompletedEvent) {
            MoneyIncreaseCompletedEvent completedEvent = (MoneyIncreaseCompletedEvent) event;
            this.balance = completedEvent.getNewBalance();
            version = event.getVersion();
        } else if (event instanceof MoneyIncreaseFailedEvent) {
            // 실패 이벤트는 상태를 변경하지 않음 (로깅 목적)
            version = event.getVersion();
        }
    }
    
    /**
     * 커밋되지 않은 이벤트들 반환
     */
    public List<MoneyDomainEvent> getUncommittedEvents() {
        return new ArrayList<>(uncommittedEvents);
    }
    
    /**
     * 커밋되지 않은 이벤트들 클리어
     */
    public void markEventsAsCommitted() {
        uncommittedEvents.clear();
    }
    
    /**
     * 현재 버전 반환
     */
    public long getCurrentVersion() {
        return version;
    }
}
