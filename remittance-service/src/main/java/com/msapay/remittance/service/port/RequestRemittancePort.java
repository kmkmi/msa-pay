package com.msapay.remittance.service.port;

import com.msapay.remittance.persistence.RemittanceRequestJpaEntity;
import com.msapay.remittance.controller.command.RequestRemittanceCommand;

public interface RequestRemittancePort {

    RemittanceRequestJpaEntity createRemittanceRequestHistory(RequestRemittanceCommand command);
    boolean saveRemittanceRequestHistory(RemittanceRequestJpaEntity entity);
}
