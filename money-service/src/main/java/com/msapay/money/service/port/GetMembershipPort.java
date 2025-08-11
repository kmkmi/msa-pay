package com.msapay.money.service.port;

import com.msapay.money.domain.MembershipStatus;

public interface GetMembershipPort {
    public MembershipStatus getMembership(String membershipId);
}
