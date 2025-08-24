package com.msapay.banking.outbound.external.bank;

import lombok.Data;

@Data
public class BankAccount {
    private String bankName;
    private String bankAccountNumber;

    private boolean valid;

    public BankAccount(String bankName, String bankAccountNumber, boolean valid) {
        this.bankName = bankName;
        this.bankAccountNumber = bankAccountNumber;
        this.valid = valid;
    }
}
