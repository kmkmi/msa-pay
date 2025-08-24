package com.msapay.banking.persistence;

import com.msapay.banking.controller.command.GetRegisteredBankAccountCommand;
import com.msapay.banking.service.port.GetRegisteredBankAccountPort;
import com.msapay.banking.service.port.RegisterBankAccountPort;
import com.msapay.banking.domain.RegisteredBankAccount;
import com.msapay.common.PersistenceAdapter;
import lombok.RequiredArgsConstructor;

import java.util.List;

@PersistenceAdapter
@RequiredArgsConstructor
public class RegisteredBankAccountPersistenceAdapter implements RegisterBankAccountPort, GetRegisteredBankAccountPort {

    private final SpringDataRegisteredBankAccountRepository bankAccountRepository;

    @Override
    public RegisteredBankAccountJpaEntity createRegisteredBankAccount(RegisteredBankAccount.MembershipId membershipId, RegisteredBankAccount.BankName bankName, RegisteredBankAccount.BankAccountNumber bankAccountNumber, RegisteredBankAccount.LinkedStatusvalid linkedStatusvalid, RegisteredBankAccount.AggregateIdentifier aggregateIdentifier) {
        return bankAccountRepository.save(
                new RegisteredBankAccountJpaEntity(
                        membershipId.getMembershipId(),
                        bankName.getBankName(),
                        bankAccountNumber.getBankAccountNumber(),
                        linkedStatusvalid.isLinkedStatusvalid(),
                        aggregateIdentifier.getAggregateIdentifier()
                )
        );
    }

    @Override
    public RegisteredBankAccountJpaEntity getRegisteredBankAccount(GetRegisteredBankAccountCommand command) {
        List<RegisteredBankAccountJpaEntity> entityList = bankAccountRepository.findByMembershipId(command.getMembershipId());
        if (entityList.size() > 0) {
            return entityList.get(0);
        }
        return null;
    }
}
