package com.msapay.banking.domain.event;

import lombok.Getter;

/**
 * 뱅킹 계좌 등록 이벤트
 */
@Getter
public class BankAccountRegisteredEvent extends BankingDomainEvent {
    
    private final String membershipId;
    private final String bankName;
    private final String bankAccountNumber;
    private final boolean valid;
    
    public BankAccountRegisteredEvent(String aggregateId, String membershipId, String bankName, 
                                    String bankAccountNumber, boolean valid, long version) {
        super(aggregateId, "BankAccountRegistered", version);
        this.membershipId = membershipId;
        this.bankName = bankName;
        this.bankAccountNumber = bankAccountNumber;
        this.valid = valid;
    }
    
    @Override
    public String getEventType() {
        return "BankAccountRegistered";
    }
}
