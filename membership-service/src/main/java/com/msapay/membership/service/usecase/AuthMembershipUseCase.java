package com.msapay.membership.service.usecase;


import com.msapay.membership.controller.command.LoginMembershipCommand;
import com.msapay.membership.controller.command.RefreshTokenCommand;
import com.msapay.membership.controller.command.ValidateTokenCommand;
import com.msapay.membership.domain.JwtToken;
import com.msapay.membership.domain.Membership;

public interface AuthMembershipUseCase {
    JwtToken loginMembership(LoginMembershipCommand command);

    JwtToken refreshJwtTokenByRefreshToken(RefreshTokenCommand command);
    boolean validateJwtToken(ValidateTokenCommand command);

    Membership getMembershipByJwtToken(ValidateTokenCommand command);
}
