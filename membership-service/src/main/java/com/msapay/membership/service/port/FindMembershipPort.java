package com.msapay.membership.service.port;

import com.msapay.membership.persistence.MembershipJpaEntity;
import com.msapay.membership.domain.Membership;

import java.util.List;

public interface FindMembershipPort {
    MembershipJpaEntity findMembership(
            Membership.MembershipId membershipId
    );

    List<MembershipJpaEntity> findMembershipListByAddress(
            Membership.MembershipAddress membershipAddress
    );
}
