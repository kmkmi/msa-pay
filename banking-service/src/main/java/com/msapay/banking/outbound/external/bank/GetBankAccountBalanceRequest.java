package com.msapay.banking.outbound.external.bank;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GetBankAccountBalanceRequest {
    private String bankName;
    private String bankAccountNumber;
}