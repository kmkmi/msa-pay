package com.msapay.banking.domain.event;

import lombok.Getter;

/**
 * 펌뱅킹 요청 이벤트
 */
@Getter
public class FirmbankingRequestedEvent extends BankingDomainEvent {
    
    private final String membershipId;
    private final String fromBankName;
    private final String fromBankAccountNumber;
    private final String toBankName;
    private final String toBankAccountNumber;
    private final int moneyAmount;
    private final String firmbankingStatus;
    
    public FirmbankingRequestedEvent(String aggregateId, String membershipId, String fromBankName,
                                   String fromBankAccountNumber, String toBankName, String toBankAccountNumber,
                                   int moneyAmount, String firmbankingStatus, long version) {
        super(aggregateId, "FirmbankingRequested", version);
        this.membershipId = membershipId;
        this.fromBankName = fromBankName;
        this.fromBankAccountNumber = fromBankAccountNumber;
        this.toBankName = toBankName;
        this.toBankAccountNumber = toBankAccountNumber;
        this.moneyAmount = moneyAmount;
        this.firmbankingStatus = firmbankingStatus;
    }
    
    @Override
    public String getEventType() {
        return "FirmbankingRequested";
    }
}
