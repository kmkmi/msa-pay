package com.msapay.remittance.service.usecase;


import com.msapay.remittance.controller.command.FindRemittanceCommand;
import com.msapay.remittance.domain.RemittanceRequest;
import java.util.List;

public interface FindRemittanceUseCase {
    List<RemittanceRequest> findRemittanceHistory(FindRemittanceCommand command);
}
