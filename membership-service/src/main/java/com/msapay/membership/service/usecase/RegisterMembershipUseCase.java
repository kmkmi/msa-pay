package com.msapay.membership.service.usecase;


import com.msapay.membership.controller.command.RegisterMembershipCommand;
import com.msapay.membership.domain.Membership;

public interface RegisterMembershipUseCase {
    Membership registerMembership(RegisterMembershipCommand command);
}
