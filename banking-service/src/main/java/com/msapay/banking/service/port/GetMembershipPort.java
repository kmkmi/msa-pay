package com.msapay.banking.service.port;

import com.msapay.banking.domain.MembershipStatus;

public interface GetMembershipPort {
    public MembershipStatus getMembership(String membershipId);
}
