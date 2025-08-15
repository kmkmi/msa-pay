package com.msapay.membership.service;

import com.msapay.common.UseCase;
import com.msapay.membership.persistence.MembershipDto;
import com.msapay.membership.persistence.MembershipMapper;
import com.msapay.membership.controller.command.RegisterMembershipCommand;
import com.msapay.membership.service.usecase.RegisterMembershipUseCase;
import com.msapay.membership.service.port.RegisterMembershipPort;
import com.msapay.membership.domain.Membership;
import lombok.RequiredArgsConstructor;

import javax.transaction.Transactional;

@UseCase
@RequiredArgsConstructor
@Transactional
public class RegisterMembershipService implements RegisterMembershipUseCase {

    private final RegisterMembershipPort registerMembershipPort;
    private final MembershipMapper membershipMapper;
    @Override
    public Membership registerMembership(RegisterMembershipCommand command) {
        MembershipDto membershipDto = registerMembershipPort.createMembership(
                new Membership.MembershipName(command.getName()),
                new Membership.MembershipEmail(command.getEmail()),
                new Membership.MembershipAddress(command.getAddress()),
                new Membership.MembershipValid(command.isValid()),
                new Membership.MembershipCorp(command.isCorp())
        );
        // entity -> Membership
        return membershipMapper.mapToDto(membershipDto);
    }

}
