package com.msapay.membership.service.port;

import com.msapay.membership.persistence.MembershipJpaEntity;
import com.msapay.membership.domain.Membership;

public interface ModifyMembershipPort {

    MembershipJpaEntity modifyMembership(
            Membership.MembershipId membershipId
        ,Membership.MembershipName membershipName
        , Membership.MembershipEmail membershipEmail
        , Membership.MembershipAddress membershipAddress
        , Membership.MembershipIsValid membershipIsValid
            ,Membership.MembershipIsCorp membershipIsCorp
            ,Membership.MembershipRefreshToken membershipRefreshToken
    );
}
