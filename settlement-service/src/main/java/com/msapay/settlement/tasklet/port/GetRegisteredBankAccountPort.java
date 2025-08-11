package com.msapay.settlement.tasklet.port;

public interface GetRegisteredBankAccountPort {
    RegisteredBankAccountAggregateIdentifier getRegisteredBankAccount(String membershipId);

    // 타겟 계좌, 금액
    void requestFirmbanking(String bankName, String bankAccountNumber, int moneyAmount);
}
