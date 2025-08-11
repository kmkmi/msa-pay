package com.msapay.remittance.service;

import com.msapay.common.UseCase;
import com.msapay.remittance.persistence.RemittanceRequestMapper;
import com.msapay.remittance.controller.command.FindRemittanceCommand;
import com.msapay.remittance.service.usecase.FindRemittanceUseCase;
import com.msapay.remittance.service.port.FindRemittancePort;
import com.msapay.remittance.domain.RemittanceRequest;
import lombok.RequiredArgsConstructor;

import javax.transaction.Transactional;
import java.util.List;

@UseCase
@RequiredArgsConstructor
@Transactional
public class FindRemittanceService implements FindRemittanceUseCase {
    private final FindRemittancePort findRemittancePort;
    private final RemittanceRequestMapper mapper;

    @Override
    public List<RemittanceRequest> findRemittanceHistory(FindRemittanceCommand command) {
        //
        findRemittancePort.findRemittanceHistory(command);
        return null;
    }
}
