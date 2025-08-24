package com.msapay.banking.controller;

import com.msapay.banking.controller.command.GetBankAccountBalanceCommand;
import com.msapay.banking.controller.command.GetRegisteredBankAccountCommand;
import com.msapay.banking.controller.request.GetBankAccountBalanceRequest;
import com.msapay.banking.controller.request.RegisterBankAccountRequest;
import com.msapay.banking.service.usecase.GetBankAccountBalanceUseCase;
import com.msapay.banking.service.usecase.GetRegisteredBankAccountUseCase;
import com.msapay.banking.domain.RegisteredBankAccount;
import com.msapay.banking.service.usecase.VerifyCorpAccountUseCase;
import com.msapay.common.WebAdapter;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;


@WebAdapter
@RestController
@RequiredArgsConstructor
public class GetRegisteredBankAccountController {

    private final GetRegisteredBankAccountUseCase getRegisteredBankAccountUseCase;
    private final GetBankAccountBalanceUseCase getBankAccountBalanceUseCase;
    private final VerifyCorpAccountUseCase verifyCorpAccountUseCase;

    @GetMapping(path = "/banking/account/{membershipId}")
    RegisteredBankAccount getRegisteredBankAccount(@PathVariable String membershipId) {
        GetRegisteredBankAccountCommand command = GetRegisteredBankAccountCommand.builder().membershipId(membershipId).build();
        return getRegisteredBankAccountUseCase.getRegisteredBankAccount(command);
    }

    @PostMapping(path = "/banking/account/balance")
    long getBankAccountBalance(@RequestBody GetBankAccountBalanceRequest request) {
        GetBankAccountBalanceCommand command = GetBankAccountBalanceCommand.builder().bankName(request.getBankName()).bankAccountNumber(request.getBankAccountNumber()).build();
        return getBankAccountBalanceUseCase.getBankAccountBalance(command);
    }

    @GetMapping(path = "/banking/verifyCorpAccount")
    boolean verifyCorpAccount() {
        return verifyCorpAccountUseCase.verifyCorpAccount();
    }
}
