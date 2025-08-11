package com.msapay.banking.service.usecase;


import com.msapay.banking.domain.FirmbankingRequest;
import com.msapay.banking.controller.command.RequestFirmbankingCommand;

public interface RequestFirmbankingUseCase {
    FirmbankingRequest requestFirmbanking(RequestFirmbankingCommand command);
    void requestFirmbankingByEvent(RequestFirmbankingCommand command);
}
