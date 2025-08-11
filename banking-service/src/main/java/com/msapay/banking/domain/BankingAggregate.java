package com.msapay.banking.domain;

import com.msapay.banking.domain.event.BankingDomainEvent;
import com.msapay.banking.domain.event.BankAccountRegisteredEvent;
import com.msapay.banking.domain.event.FirmbankingRequestedEvent;
import com.msapay.banking.domain.event.FirmbankingCompletedEvent;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

/**
 * 뱅킹 어그리게이트 루트 - 이벤트 소싱 패턴 구현
 */
@Getter
public class BankingAggregate {
    
    private String aggregateId;
    private String membershipId;
    private String bankName;
    private String bankAccountNumber;
    private boolean valid;
    private long version;
    private List<BankingDomainEvent> uncommittedEvents = new ArrayList<>();
    
    public BankingAggregate(String aggregateId, String membershipId) {
        this.aggregateId = aggregateId;
        this.membershipId = membershipId;
        this.valid = false;
        this.version = 0;
    }
    
    /**
     * 이벤트 스토어에서 이벤트들을 재생하여 어그리게이트 상태 복원
     */
    public static BankingAggregate fromEvents(String aggregateId, List<BankingDomainEvent> events) {
        BankingAggregate aggregate = new BankingAggregate(aggregateId, "");
        
        for (BankingDomainEvent event : events) {
            aggregate.applyEvent(event);
        }
        
        return aggregate;
    }
    
    /**
     * 뱅킹 계좌 등록 처리
     */
    public void registerBankAccount(String membershipId, String bankName, String bankAccountNumber, boolean valid) {
        BankAccountRegisteredEvent event = new BankAccountRegisteredEvent(
            aggregateId, membershipId, bankName, bankAccountNumber, valid, version + 1
        );
        
        applyEvent(event);
        uncommittedEvents.add(event);
    }
    
    /**
     * 펌뱅킹 요청 처리
     */
    public void requestFirmbanking(String membershipId, String fromBankName, String fromBankAccountNumber,
                                 String toBankName, String toBankAccountNumber, int moneyAmount, String firmbankingStatus) {
        FirmbankingRequestedEvent event = new FirmbankingRequestedEvent(
            aggregateId, membershipId, fromBankName, fromBankAccountNumber, 
            toBankName, toBankAccountNumber, moneyAmount, firmbankingStatus, version + 1
        );
        
        applyEvent(event);
        uncommittedEvents.add(event);
    }
    
    /**
     * 펌뱅킹 완료 처리
     */
    public void completeFirmbanking(String membershipId, String fromBankName, String fromBankAccountNumber,
                                  String toBankName, String toBankAccountNumber, int moneyAmount, 
                                  String firmbankingStatus, String resultCode) {
        FirmbankingCompletedEvent event = new FirmbankingCompletedEvent(
            aggregateId, membershipId, fromBankName, fromBankAccountNumber, 
            toBankName, toBankAccountNumber, moneyAmount, firmbankingStatus, resultCode, version + 1
        );
        
        applyEvent(event);
        uncommittedEvents.add(event);
    }
    
    /**
     * 이벤트 적용하여 상태 변경
     */
    public void applyEvent(BankingDomainEvent event) {
        if (event instanceof BankAccountRegisteredEvent) {
            BankAccountRegisteredEvent registeredEvent = (BankAccountRegisteredEvent) event;
            this.membershipId = registeredEvent.getMembershipId();
            this.bankName = registeredEvent.getBankName();
            this.bankAccountNumber = registeredEvent.getBankAccountNumber();
            this.valid = registeredEvent.isValid();
            version = event.getVersion();
        } else if (event instanceof FirmbankingRequestedEvent) {
            // 펌뱅킹 요청 이벤트는 상태를 변경하지 않음 (로깅 목적)
            version = event.getVersion();
        } else if (event instanceof FirmbankingCompletedEvent) {
            // 펌뱅킹 완료 이벤트는 상태를 변경하지 않음 (로깅 목적)
            version = event.getVersion();
        }
    }
    
    /**
     * 커밋되지 않은 이벤트들 반환
     */
    public List<BankingDomainEvent> getUncommittedEvents() {
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
