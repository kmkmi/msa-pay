package com.msapay.banking.service;

import com.msapay.banking.outbound.external.bank.BankAccount;
import com.msapay.banking.outbound.external.bank.GetBankAccountRequest;
import com.msapay.banking.domain.MembershipStatus;
import com.msapay.banking.persistence.RegisteredBankAccountJpaEntity;
import com.msapay.banking.persistence.RegisteredBankAccountMapper;
import com.msapay.banking.domain.RegisteredBankAccount;
import com.msapay.banking.domain.BankingAggregate;
import com.msapay.banking.domain.repository.BankingAggregateRepository;
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

import javax.transaction.Transactional;

@UseCase
@RequiredArgsConstructor
@Transactional
public class RegisterBankAccountService implements RegisterBankAccountUseCase, GetRegisteredBankAccountUseCase {

    private final GetMembershipPort getMembershipPort;
    private final RegisterBankAccountPort registerBankAccountPort;
    private final RegisteredBankAccountMapper mapper;
    private final RequestBankAccountInfoPort requestBankAccountInfoPort;
    private final GetRegisteredBankAccountPort getRegisteredBankAccountPort;
    private final BankingAggregateRepository bankingAggregateRepository;
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
        boolean accountIsValid =  accountInfo.isValid();

        // 2. 등록가능한 계좌라면, 등록한다. 성공하면, 등록에 성공한 등록 정보를 리턴
        // 2-1. 등록가능하지 않은 계좌라면. 에러를 리턴
        if(accountIsValid) {
            // 등록 정보 저장
            RegisteredBankAccountJpaEntity savedAccountInfo = registerBankAccountPort.createRegisteredBankAccount(
                    new RegisteredBankAccount.MembershipId(command.getMembershipId()+""),
                    new RegisteredBankAccount.BankName(command.getBankName()),
                    new RegisteredBankAccount.BankAccountNumber(command.getBankAccountNumber()),
                    new RegisteredBankAccount.LinkedStatusIsValid(command.isValid()),
                    new RegisteredBankAccount.AggregateIdentifier(""));

            return mapper.mapToDomainEntity(savedAccountInfo);
        } else {
            return null;
        }
    }

    @Override
    public void registerBankAccountByEvent(RegisterBankAccountCommand command) {
        try {
            // 이벤트 소싱 기반으로 뱅킹 계좌 등록
            String aggregateId = "banking-" + command.getMembershipId();
            BankingAggregate aggregate = new BankingAggregate(aggregateId, command.getMembershipId());
            
            // 뱅킹 계좌 등록 이벤트 발생
            aggregate.registerBankAccount(
                command.getMembershipId(), 
                command.getBankName(), 
                command.getBankAccountNumber(), 
                command.isValid()
            );
            
            // 어그리게이트 저장 (이벤트 저장)
            bankingAggregateRepository.save(aggregate).join();
            
            // 기존 포트를 통한 뱅킹 계좌 등록
            RegisteredBankAccountJpaEntity savedAccountInfo = registerBankAccountPort.createRegisteredBankAccount(
                new RegisteredBankAccount.MembershipId(command.getMembershipId()+""),
                new RegisteredBankAccount.BankName(command.getBankName()),
                new RegisteredBankAccount.BankAccountNumber(command.getBankAccountNumber()),
                new RegisteredBankAccount.LinkedStatusIsValid(command.isValid()),
                new RegisteredBankAccount.AggregateIdentifier(aggregateId)
            );
            
            System.out.println("Bank account registered successfully with aggregate ID: " + aggregateId);
            
        } catch (Exception e) {
            System.err.println("Failed to register bank account for membership: " + command.getMembershipId());
            e.printStackTrace();
            throw new RuntimeException("Bank account registration failed", e);
        }
    }

    @Override
    public RegisteredBankAccount getRegisteredBankAccount(GetRegisteredBankAccountCommand command) {
        return mapper.mapToDomainEntity(getRegisteredBankAccountPort.getRegisteredBankAccount(command));
    }
}
