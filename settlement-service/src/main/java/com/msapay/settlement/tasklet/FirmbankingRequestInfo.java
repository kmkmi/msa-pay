package com.msapay.settlement.tasklet;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@NoArgsConstructor
@AllArgsConstructor
public class FirmbankingRequestInfo {

    private String bankName;
    private String bankAccountNumber;
    private int moneyAmount;

    public FirmbankingRequestInfo(String bankName, String bankAccountNumber) {
        this.bankName = bankName;
        this.bankAccountNumber = bankAccountNumber;
    }
}