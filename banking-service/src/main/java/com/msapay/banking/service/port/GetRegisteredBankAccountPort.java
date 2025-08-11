package com.msapay.banking.service.port;

import com.msapay.banking.persistence.RegisteredBankAccountJpaEntity;
import com.msapay.banking.controller.command.GetRegisteredBankAccountCommand;

public interface GetRegisteredBankAccountPort {
    RegisteredBankAccountJpaEntity getRegisteredBankAccount(GetRegisteredBankAccountCommand command);
}
