package com.msapay.banking.service.port;

import com.msapay.banking.outbound.external.bank.BankAccount;
import com.msapay.banking.outbound.external.bank.GetBankAccountRequest;

public interface RequestBankAccountInfoPort {
    BankAccount getBankAccountInfo(GetBankAccountRequest request) ;
}
