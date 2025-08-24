package com.msapay.money.service.port;

import com.msapay.money.domain.RegisteredBankAccountAggregateIdentifier;
import org.springframework.web.bind.annotation.RequestBody;

public interface BankingServicePort {
    RegisteredBankAccountAggregateIdentifier getRegisteredBankAccount(String membershipId);
    boolean requestFirmbanking(String bankName, String bankAccountNumber, int amount);
    long getBanckAccountBalance(String bankName, String bankAccountNumber);
    boolean verifyCorpAccount();
}
