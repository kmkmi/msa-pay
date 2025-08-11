package com.msapay.banking.domain.event;

import lombok.Getter;

/**
 * 펌뱅킹 완료 이벤트
 */
@Getter
public class FirmbankingCompletedEvent extends BankingDomainEvent {
    
    private final String membershipId;
    private final String fromBankName;
    private final String fromBankAccountNumber;
    private final String toBankName;
    private final String toBankAccountNumber;
    private final int moneyAmount;
    private final String firmbankingStatus;
    private final String resultCode;
    
    public FirmbankingCompletedEvent(String aggregateId, String membershipId, String fromBankName,
                                   String fromBankAccountNumber, String toBankName, String toBankAccountNumber,
                                   int moneyAmount, String firmbankingStatus, String resultCode, long version) {
        super(aggregateId, "FirmbankingCompleted", version);
        this.membershipId = membershipId;
        this.fromBankName = fromBankName;
        this.fromBankAccountNumber = fromBankAccountNumber;
        this.toBankName = toBankName;
        this.toBankAccountNumber = toBankAccountNumber;
        this.moneyAmount = moneyAmount;
        this.firmbankingStatus = firmbankingStatus;
        this.resultCode = resultCode;
    }
    
    @Override
    public String getEventType() {
        return "FirmbankingCompleted";
    }
}
