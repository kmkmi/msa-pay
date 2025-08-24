package com.msapay.banking.service.port;

import com.msapay.banking.persistence.RegisteredBankAccountJpaEntity;
import com.msapay.banking.domain.RegisteredBankAccount;

public interface RegisterBankAccountPort {

    RegisteredBankAccountJpaEntity createRegisteredBankAccount(
            RegisteredBankAccount.MembershipId membershipId,
            RegisteredBankAccount.BankName bankName,
            RegisteredBankAccount.BankAccountNumber bankAccountNumber,
            RegisteredBankAccount.LinkedStatusvalid linkedStatusvalid,
            RegisteredBankAccount.AggregateIdentifier aggregateIdentifier
    );
}
