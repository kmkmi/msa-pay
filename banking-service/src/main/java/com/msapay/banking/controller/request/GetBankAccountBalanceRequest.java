package com.msapay.banking.controller.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class GetBankAccountBalanceRequest {
    private String bankName;
    private String bankAccountNumber;
}
