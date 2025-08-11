package com.msapay.remittance.service.port;

import com.msapay.remittance.persistence.RemittanceRequestJpaEntity;
import com.msapay.remittance.controller.command.FindRemittanceCommand;

import java.util.List;

public interface FindRemittancePort {

    List<RemittanceRequestJpaEntity> findRemittanceHistory(FindRemittanceCommand command);
}
