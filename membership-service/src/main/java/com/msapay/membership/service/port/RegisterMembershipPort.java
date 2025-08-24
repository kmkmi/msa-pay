package com.msapay.membership.service.port;

import com.msapay.membership.persistence.MembershipDto;
import com.msapay.membership.persistence.MembershipJpaEntity;
import com.msapay.membership.domain.Membership;

public interface RegisterMembershipPort {

    MembershipDto createMembership(
        Membership.MembershipName membershipName
        , Membership.MembershipEmail membershipEmail
        , Membership.MembershipAddress membershipAddress
        , Membership.MembershipValid membershipvalid
            ,Membership.MembershipCorp membershipcorp
    );
}
