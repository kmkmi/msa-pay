package com.msapay.banking.service.usecase;


import com.msapay.banking.domain.RegisteredBankAccount;
import com.msapay.banking.controller.command.GetRegisteredBankAccountCommand;

public interface GetRegisteredBankAccountUseCase {
    RegisteredBankAccount getRegisteredBankAccount(GetRegisteredBankAccountCommand command);
}
