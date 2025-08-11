package com.msapay.banking.controller;

import com.msapay.banking.controller.command.RegisterBankAccountCommand;
import com.msapay.banking.service.usecase.RegisterBankAccountUseCase;
import com.msapay.banking.controller.request.RegisterBankAccountRequest;
import com.msapay.banking.domain.RegisteredBankAccount;
import com.msapay.common.WebAdapter;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;


@WebAdapter
@RestController
@RequiredArgsConstructor
public class RegisterBankAccountController {

    private final RegisterBankAccountUseCase registeredBankAccountUseCase;
    @PostMapping(path = "/banking/account/register")
    RegisteredBankAccount registeredBankAccount(@RequestBody RegisterBankAccountRequest request) {
        RegisterBankAccountCommand command = RegisterBankAccountCommand.builder()
                .membershipId(request.getMembershipId())
                .bankName(request.getBankName())
                .bankAccountNumber(request.getBankAccountNumber())
                .isValid(request.isValid())
                .build();

        return registeredBankAccountUseCase.registerBankAccount(command);
    }

//    @PostMapping(path = "/banking/account/register-eda")
//    void registeredBankAccountByEvent(@RequestBody RegisterBankAccountRequest request) {
//        RegisterBankAccountCommand command = RegisterBankAccountCommand.builder()
//                .membershipId(request.getMembershipId())
//                .bankName(request.getBankName())
//                .bankAccountNumber(request.getBankAccountNumber())
//                .isValid(request.isValid())
//                .build();
//
//        registeredBankAccountUseCase.registerBankAccountByEvent(command);
//    }
}
