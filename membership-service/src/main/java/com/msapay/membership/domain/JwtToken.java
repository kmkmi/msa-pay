package com.msapay.membership.domain;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Value;

@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class JwtToken {
    @Getter private final String membershipId;
    @Getter private final String jwtToken;
    @Getter private final String refreshToken;

    public static JwtToken generateJwtToken (
            MembershipId membershipId
            , MembershipJwtToken membershipJwtToken
            , MembershipRefreshToken membershipRefreshToken
    ){
        return new JwtToken(
                membershipId.membershipId,
                membershipJwtToken.jwtToken,
                membershipRefreshToken.refreshToken
        );
    }

    @Value
    public static class MembershipId {
        String membershipId;
    }

    @Value
    public static class MembershipJwtToken {
        String jwtToken;
    }

    @Value
    public static class MembershipRefreshToken {
        String refreshToken;
    }
}
