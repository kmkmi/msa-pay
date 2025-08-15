package com.msapay.membership.service;

import com.msapay.common.UseCase;
import com.msapay.membership.persistence.MembershipDto;
import com.msapay.membership.persistence.MembershipJpaEntity;
import com.msapay.membership.persistence.MembershipMapper;
import com.msapay.membership.controller.command.ModifyMembershipCommand;
import com.msapay.membership.service.usecase.ModifyMembershipUseCase;
import com.msapay.membership.service.port.ModifyMembershipPort;
import com.msapay.membership.domain.Membership;
import lombok.RequiredArgsConstructor;

import javax.transaction.Transactional;

@UseCase
@RequiredArgsConstructor
@Transactional
public class ModifyMembershipService implements ModifyMembershipUseCase {

    private final ModifyMembershipPort modifyMembershipPort;
    private final MembershipMapper membershipMapper;

    @Override
    public Membership modifyMembership(ModifyMembershipCommand command) {
        MembershipDto membershipDto = modifyMembershipPort.modifyMembership(
                new Membership.MembershipId(command.getMembershipId()),
                new Membership.MembershipName(command.getName()),
                new Membership.MembershipEmail(command.getEmail()),
                new Membership.MembershipAddress(command.getAddress()),
                new Membership.MembershipValid(command.isValid()),
                new Membership.MembershipCorp(command.isCorp()),
                new Membership.MembershipRefreshToken("")
        );

        // entity -> Membership
        return membershipMapper.mapToDto(membershipDto);
    }
}
