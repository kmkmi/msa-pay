package com.msapay.banking.service;


import com.msapay.banking.controller.command.GetBankAccountBalanceCommand;
import com.msapay.banking.controller.request.GetBankAccountBalanceRequest;
import com.msapay.banking.outbound.external.bank.ExternalFirmbankingRequest;
import com.msapay.banking.outbound.external.bank.FirmbankingResult;
import com.msapay.banking.persistence.FirmbankingRequestJpaEntity;
import com.msapay.banking.persistence.FirmbankingRequestMapper;
import com.msapay.banking.controller.command.RequestFirmbankingCommand;
import com.msapay.banking.service.usecase.GetBankAccountBalanceUseCase;
import com.msapay.banking.service.usecase.RequestFirmbankingUseCase;
import com.msapay.banking.service.port.RequestExternalFirmbankingPort;
import com.msapay.banking.service.port.RequestFirmbankingPort;
import com.msapay.banking.domain.FirmbankingRequest;
import com.msapay.banking.service.usecase.VerifyCorpAccountUseCase;
import com.msapay.common.UseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import javax.transaction.Transactional;
import java.util.UUID;

@Slf4j
@UseCase
@RequiredArgsConstructor
@Transactional
public class RequestFirmbankingService implements RequestFirmbankingUseCase, GetBankAccountBalanceUseCase, VerifyCorpAccountUseCase {
    private final FirmbankingRequestMapper mapper;
    private final RequestFirmbankingPort requestFirmbankingPort;
    private final RequestExternalFirmbankingPort requestExternalFirmbankingPort;
//    private final CommandGateway commandGateway;
    @Override
    public FirmbankingRequest requestFirmbanking(RequestFirmbankingCommand command) {

        // Business Logic
        // a -> b 계좌

        // 1. 요청에 대해 정보를 먼저 write . "요청" 상태로
        FirmbankingRequestJpaEntity requestedEntity = requestFirmbankingPort.createFirmbankingRequest(
                new FirmbankingRequest.FromBankName(command.getFromBankName()),
                new FirmbankingRequest.FromBankAccountNumber(command.getFromBankAccountNumber()),
                new FirmbankingRequest.ToBankName(command.getToBankName()),
                new FirmbankingRequest.ToBankAccountNumber(command.getToBankAccountNumber()),
                new FirmbankingRequest.MoneyAmount(command.getMoneyAmount()),
                new FirmbankingRequest.FirmbankingStatus(0),
                new FirmbankingRequest.FirmbankingAggregateIdentifier("")
        );

        // 2. 외부 은행에 펌뱅킹 요청
        // 외부 은행 펌뱀킹 mocking
        FirmbankingResult result = requestExternalFirmbankingPort.requestExternalFirmbanking(new ExternalFirmbankingRequest(
                command.getFromBankName(),
                command.getFromBankAccountNumber(),
                command.getToBankName(),
                command.getToBankAccountNumber(),
                command.getMoneyAmount()
        ));

        // Transactional UUID
        UUID randomUUID = UUID.randomUUID();
        requestedEntity.setUuid(randomUUID.toString());

        // 3. 결과에 따라서 1번에서 작성했던 FirmbankingRequest 정보를 Update
        if (result.getResultCode() == 0){
            // 성공
            requestedEntity.setFirmbankingStatus(1);
        } else {
            // 실패
            requestedEntity.setFirmbankingStatus(2);
        }

        // 4. 결과를 리턴하기 전에 바뀐 상태 값을 기준으로 다시 save
        return mapper.mapToDomainEntity(requestFirmbankingPort.modifyFirmbankingRequest(requestedEntity), randomUUID);
    }

    @Override
    public long getBankAccountBalance(GetBankAccountBalanceCommand command){
        GetBankAccountBalanceRequest request = new GetBankAccountBalanceRequest(command.getBankName(), command.getBankAccountNumber());
        return requestExternalFirmbankingPort.getBankAccountBalance(request);
    }

    @Override
    public boolean verifyCorpAccount() {
        return requestExternalFirmbankingPort.verifyCorpAccount();
    }
}
