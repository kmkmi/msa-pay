package com.msapay.payment.service.port;

public interface GetMembershipPort {
    public MembershipStatus getMembership(String membershipId);
}
