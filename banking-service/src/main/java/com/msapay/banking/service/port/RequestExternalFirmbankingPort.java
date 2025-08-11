package com.msapay.banking.service.port;

import com.msapay.banking.outbound.external.bank.ExternalFirmbankingRequest;
import com.msapay.banking.outbound.external.bank.FirmbankingResult;

public interface RequestExternalFirmbankingPort {
    FirmbankingResult requestExternalFirmbanking(ExternalFirmbankingRequest request);
}
