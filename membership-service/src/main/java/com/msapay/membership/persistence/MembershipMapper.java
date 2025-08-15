package com.msapay.membership.persistence;

import com.msapay.membership.domain.Membership;
import org.springframework.stereotype.Component;

@Component
public class MembershipMapper {
    public Membership mapToDto(MembershipDto membershipDto) {
        return Membership.generateMember(
                new Membership.MembershipId(membershipDto.getMembershipId()+""),
                new Membership.MembershipName(membershipDto.getName()),
                new Membership.MembershipEmail(membershipDto.getEmail()),
                new Membership.MembershipAddress(membershipDto.getAddress()),
                new Membership.MembershipValid(membershipDto.isValid()),
                new Membership.MembershipCorp(membershipDto.isCorp()),
                new Membership.MembershipRefreshToken(membershipDto.getRefreshToken())
        );
    }
}
