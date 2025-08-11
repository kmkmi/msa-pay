package com.msapay.membership.service.usecase;


import com.msapay.membership.controller.command.ModifyMembershipCommand;
import com.msapay.membership.domain.Membership;

public interface ModifyMembershipUseCase {
    Membership modifyMembership(ModifyMembershipCommand command);
}
