package com.msapay.membership.service.port;

import com.msapay.membership.persistence.MembershipDto;
import com.msapay.membership.persistence.MembershipJpaEntity;
import com.msapay.membership.domain.Membership;

public interface ModifyMembershipPort {

    MembershipDto modifyMembership(
            Membership.MembershipId membershipId
        ,Membership.MembershipName membershipName
        , Membership.MembershipEmail membershipEmail
        , Membership.MembershipAddress membershipAddress
        , Membership.MembershipValid membershipValid
            ,Membership.MembershipCorp membershipCorp
            ,Membership.MembershipRefreshToken membershipRefreshToken
    );
}
