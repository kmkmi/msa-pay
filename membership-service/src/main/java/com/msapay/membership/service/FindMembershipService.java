package com.msapay.membership.service;

import com.msapay.common.UseCase;
import com.msapay.membership.persistence.MembershipDto;
import com.msapay.membership.persistence.MembershipMapper;
import com.msapay.membership.controller.command.FindMembershipCommand;
import com.msapay.membership.controller.command.FindMembershipListByAddressCommand;
import com.msapay.membership.service.usecase.FindMembershipUseCase;
import com.msapay.membership.service.port.FindMembershipPort;
import com.msapay.membership.domain.Membership;
import lombok.RequiredArgsConstructor;

import javax.transaction.Transactional;
import java.util.ArrayList;
import java.util.List;

@RequiredArgsConstructor
@UseCase
@Transactional
public class FindMembershipService implements FindMembershipUseCase {

    private final FindMembershipPort findMembershipPort;

    private final MembershipMapper membershipMapper;
    @Override
    public Membership findMembership(FindMembershipCommand command) {
        MembershipDto dto = findMembershipPort.findMembership(new Membership.MembershipId(command.getMembershipId()));
        return membershipMapper.mapToDto(dto);
    }

    @Override
    public List<Membership> findMembershipListByAddress(FindMembershipListByAddressCommand command) {
        List<MembershipDto> MembershipDtos = findMembershipPort.findMembershipListByAddress(new Membership.MembershipAddress(command.getAddressName()));
        List<Membership> memberships = new ArrayList<>();

        for (MembershipDto membershipDto : MembershipDtos) {
            memberships.add(membershipMapper.mapToDto(membershipDto));
        }
        return memberships;
    }
}