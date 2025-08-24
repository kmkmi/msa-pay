package com.msapay.money.outbound.service;

import com.msapay.common.CommonHttpClient;
import com.msapay.money.service.port.BankingServicePort;
import com.msapay.money.domain.RegisteredBankAccountAggregateIdentifier;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class BankingServiceAdapter implements BankingServicePort {

    private final CommonHttpClient commonHttpClient;

    private final String bankingServiceUrl;

    public BankingServiceAdapter(CommonHttpClient commonHttpClient,
                                 @Value("${service.banking.url}") String membershipServiceUrl) {
        this.commonHttpClient = commonHttpClient;
        this.bankingServiceUrl = membershipServiceUrl;
    }

    @Override
    public RegisteredBankAccountAggregateIdentifier getRegisteredBankAccount(String membershipId){
        String url = String.join("/", bankingServiceUrl, "banking/account", membershipId);
        try {
            String jsonResponse = commonHttpClient.sendGetRequest(url).body();
            ObjectMapper mapper = new ObjectMapper();
            RegisteredBankAccount registeredBankAccount = mapper.readValue(jsonResponse, RegisteredBankAccount.class);

            return new RegisteredBankAccountAggregateIdentifier(
                    registeredBankAccount.getRegisteredBankAccountId()
                    , registeredBankAccount.getAggregateIdentifier()
                    , registeredBankAccount.getMembershipId()
                    , registeredBankAccount.getBankName()
                    , registeredBankAccount.getBankAccountNumber()
            );
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public long getBanckAccountBalance(String bankName, String bankAccountNumber){
        String url = String.join("/", bankingServiceUrl, "banking/account/balance");
        try {
            Map<String, String> requestBody = new HashMap<>();
            requestBody.put("bankName", bankName);
            requestBody.put("bankAccountNumber", bankAccountNumber);
            ObjectMapper mapper = new ObjectMapper();

            String body = mapper.writeValueAsString(requestBody);

            String jsonResponse = commonHttpClient.sendPostRequest(url, body).body();

            return Long.parseLong(jsonResponse.trim());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean verifyCorpAccount(){
        String url = String.join("/", bankingServiceUrl, "banking/verifyCorpAccount");
        try {
            String jsonResponse = commonHttpClient.sendGetRequest(url).body();

            return Boolean.parseBoolean(jsonResponse.trim());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean requestFirmbanking(String bankName, String bankAccountNumber, int amount) {
        return true;
    }
}
