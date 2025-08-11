package com.msapay.money.service.port;

import com.msapay.money.domain.RegisteredBankAccountAggregateIdentifier;

public interface GetRegisteredBankAccountPort {
    RegisteredBankAccountAggregateIdentifier getRegisteredBankAccount(String membershipId);
}
