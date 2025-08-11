package com.msapay.membership.controller.command;



import com.msapay.common.SelfValidating;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@Builder
@EqualsAndHashCode(callSuper = false)
public
class LoginMembershipCommand extends SelfValidating<LoginMembershipCommand> {
    private final String membershipId;

}
