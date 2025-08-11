package com.msapay.remittance.service.usecase;


import com.msapay.remittance.controller.command.RequestRemittanceCommand;
import com.msapay.remittance.domain.RemittanceRequest;

public interface RequestRemittanceUseCase {
    RemittanceRequest requestRemittance(RequestRemittanceCommand command);
}
