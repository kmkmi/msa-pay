package com.msapay.banking.service;


import com.msapay.banking.outbound.external.bank.ExternalFirmbankingRequest;
import com.msapay.banking.outbound.external.bank.FirmbankingResult;
import com.msapay.banking.persistence.FirmbankingRequestJpaEntity;
import com.msapay.banking.persistence.FirmbankingRequestMapper;
import com.msapay.banking.controller.command.RequestFirmbankingCommand;
import com.msapay.banking.service.usecase.RequestFirmbankingUseCase;
import com.msapay.banking.controller.command.UpdateFirmbankingCommand;
import com.msapay.banking.service.usecase.UpdateFirmbankingUseCase;
import com.msapay.banking.service.port.RequestExternalFirmbankingPort;
import com.msapay.banking.service.port.RequestFirmbankingPort;
import com.msapay.banking.domain.FirmbankingRequest;
import com.msapay.common.UseCase;
import lombok.RequiredArgsConstructor;

import javax.transaction.Transactional;
import java.util.UUID;

@UseCase
@RequiredArgsConstructor
@Transactional
public class RequestFirmbankingService implements RequestFirmbankingUseCase, UpdateFirmbankingUseCase {
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
    public void requestFirmbankingByEvent(RequestFirmbankingCommand command) {
        try {
            // 이벤트 소싱 기반으로 펌뱅킹 요청
            String aggregateId = "firmbanking-" + UUID.randomUUID();
            
            // Request Firmbanking 의 DB save
            FirmbankingRequestJpaEntity requestedEntity = requestFirmbankingPort.createFirmbankingRequest(
                    new FirmbankingRequest.FromBankName(command.getFromBankName()),
                    new FirmbankingRequest.FromBankAccountNumber(command.getFromBankAccountNumber()),
                    new FirmbankingRequest.ToBankName(command.getToBankName()),
                    new FirmbankingRequest.ToBankAccountNumber(command.getToBankAccountNumber()),
                    new FirmbankingRequest.MoneyAmount(command.getMoneyAmount()),
                    new FirmbankingRequest.FirmbankingStatus(0),
                    new FirmbankingRequest.FirmbankingAggregateIdentifier(aggregateId)
            );

            // 은행에 펌뱅킹 요청
            FirmbankingResult firmbankingResult = requestExternalFirmbankingPort.requestExternalFirmbanking(new ExternalFirmbankingRequest(
                    command.getFromBankName(),
                    command.getFromBankAccountNumber(),
                    command.getToBankName(),
                    command.getToBankAccountNumber(),
                    command.getMoneyAmount()
            ));

            // 결과에 따라서 DB save
            if (firmbankingResult.getResultCode() == 0){
                // 성공
                requestedEntity.setFirmbankingStatus(1);
            } else {
                // 실패
                requestedEntity.setFirmbankingStatus(2);
            }

            // 결과를 리턴하기 전에 바뀐 상태 값을 기준으로 다시 save
            requestFirmbankingPort.modifyFirmbankingRequest(requestedEntity);
            
            System.out.println("Firmbanking request completed with aggregate ID: " + aggregateId);
            
        } catch (Exception e) {
            System.err.println("Failed to process firmbanking request");
            e.printStackTrace();
            throw new RuntimeException("Firmbanking request failed", e);
        }
    }

    @Override
    public void updateFirmbankingByEvent(UpdateFirmbankingCommand command) {
        try {
            // 펌뱅킹 상태 업데이트
            FirmbankingRequestJpaEntity entity = requestFirmbankingPort.getFirmbankingRequest(
                    new FirmbankingRequest.FirmbankingAggregateIdentifier(command.getFirmbankingAggregateIdentifier()));

            if (entity != null) {
                // status 변경
                entity.setFirmbankingStatus(command.getFirmbankingStatus());
                requestFirmbankingPort.modifyFirmbankingRequest(entity);
                
                System.out.println("Firmbanking status updated for aggregate ID: " + command.getFirmbankingAggregateIdentifier());
            } else {
                System.err.println("Firmbanking request not found for aggregate ID: " + command.getFirmbankingAggregateIdentifier());
            }
            
        } catch (Exception e) {
            System.err.println("Failed to update firmbanking request");
            e.printStackTrace();
            throw new RuntimeException("Firmbanking update failed", e);
        }
    }
}
