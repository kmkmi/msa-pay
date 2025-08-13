package com.msapay.membership.service.usecase;

import com.msapay.membership.controller.command.FindMembershipCommand;
import com.msapay.membership.controller.command.FindMembershipListByAddressCommand;
import com.msapay.membership.domain.Membership;

import java.util.List;

public interface FindMembershipUseCase {
	Membership findMembership(FindMembershipCommand command);

	List<Membership> findMembershipListByAddress(FindMembershipListByAddressCommand command);
}
