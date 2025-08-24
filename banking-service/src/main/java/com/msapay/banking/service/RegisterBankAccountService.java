package com.msapay.banking.service;

import com.msapay.banking.outbound.external.bank.BankAccount;
import com.msapay.banking.outbound.external.bank.GetBankAccountRequest;
import com.msapay.banking.domain.MembershipStatus;
import com.msapay.banking.persistence.RegisteredBankAccountJpaEntity;
import com.msapay.banking.persistence.RegisteredBankAccountMapper;
import com.msapay.banking.domain.RegisteredBankAccount;
import com.msapay.banking.controller.command.GetRegisteredBankAccountCommand;
import com.msapay.banking.controller.command.RegisterBankAccountCommand;
import com.msapay.banking.service.port.GetMembershipPort;
import com.msapay.banking.service.port.GetRegisteredBankAccountPort;
import com.msapay.banking.service.port.RegisterBankAccountPort;
import com.msapay.banking.service.port.RequestBankAccountInfoPort;
import com.msapay.banking.service.usecase.GetRegisteredBankAccountUseCase;
import com.msapay.banking.service.usecase.RegisterBankAccountUseCase;
import com.msapay.common.UseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import javax.transaction.Transactional;
import java.util.Optional;

@Slf4j
@UseCase
@RequiredArgsConstructor
@Transactional
public class RegisterBankAccountService implements RegisterBankAccountUseCase, GetRegisteredBankAccountUseCase {

    private final GetMembershipPort getMembershipPort;
    private final RegisterBankAccountPort registerBankAccountPort;
    private final RegisteredBankAccountMapper mapper;
    private final RequestBankAccountInfoPort requestBankAccountInfoPort;
    private final GetRegisteredBankAccountPort getRegisteredBankAccountPort;
//    private final CommandGateway commandGateway;
    @Override
    public RegisteredBankAccount registerBankAccount(RegisterBankAccountCommand command) {

        // 은행 계좌를 등록해야하는 서비스 (비즈니스 로직)
        // command.getMembershipId()

        // call membership svc, 정상인지 확인
        // call external bank svc, 정상인지 확인
        MembershipStatus membershipStatus = getMembershipPort.getMembership(command.getMembershipId());
        if(!membershipStatus.isValid()) {
            return null;
        }

        // 1. 외부 실제 은행에 등록이 가능한 계좌인지(정상인지) 확인한다.
        // 외부의 은행에 이 계좌 정상인지? 확인을 해야해요.
        // Biz Logic -> External System
        // Port -> Adapter -> External System
        // Port
        // 실제 외부의 은행계좌 정보를 Get
        BankAccount accountInfo = requestBankAccountInfoPort.getBankAccountInfo(new GetBankAccountRequest(command.getBankName(), command.getBankAccountNumber()));
        boolean accountvalid =  accountInfo.isValid();

        // 2. 등록가능한 계좌라면, 등록한다. 성공하면, 등록에 성공한 등록 정보를 리턴
        // 2-1. 등록가능하지 않은 계좌라면. 에러를 리턴
        if(accountvalid) {
            // 등록 정보 저장
            RegisteredBankAccountJpaEntity savedAccountInfo = registerBankAccountPort.createRegisteredBankAccount(
                    new RegisteredBankAccount.MembershipId(command.getMembershipId()+""),
                    new RegisteredBankAccount.BankName(command.getBankName()),
                    new RegisteredBankAccount.BankAccountNumber(command.getBankAccountNumber()),
                    new RegisteredBankAccount.LinkedStatusvalid(command.isValid()),
                    new RegisteredBankAccount.AggregateIdentifier(""));

            return mapper.mapToDomainEntity(savedAccountInfo);
        } else {
            return null;
        }
    }

    @Override
    public RegisteredBankAccount getRegisteredBankAccount(GetRegisteredBankAccountCommand command) {
        return Optional.ofNullable(getRegisteredBankAccountPort.getRegisteredBankAccount(command))
                .map(mapper::mapToDomainEntity)
                .orElse(null);
    }
}
