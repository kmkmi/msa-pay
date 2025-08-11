package com.msapay.banking.service.usecase;


import com.msapay.banking.controller.command.UpdateFirmbankingCommand;

public interface UpdateFirmbankingUseCase {
   void updateFirmbankingByEvent(UpdateFirmbankingCommand command);
}
