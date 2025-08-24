package com.msapay.banking.service.usecase;


import com.msapay.banking.domain.RegisteredBankAccount;
import com.msapay.banking.controller.command.RegisterBankAccountCommand;

public interface RegisterBankAccountUseCase {
    RegisteredBankAccount registerBankAccount(RegisterBankAccountCommand command);
}
