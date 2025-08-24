package com.msapay.banking.service.usecase;

import com.msapay.banking.controller.command.GetBankAccountBalanceCommand;

public interface GetBankAccountBalanceUseCase {
    long getBankAccountBalance(GetBankAccountBalanceCommand command);
}
