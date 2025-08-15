package com.msapay.membership.domain;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Value;

@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Membership {
    @Getter private final String membershipId;
    @Getter private final String name;
    @Getter private final String email;
    @Getter private final String address;
    @Getter private final boolean valid;
    @Getter private final boolean corp;

    @Getter private final String refreshToken;
    // Membership
    // 오염이 되면 안되는 클래스. 고객 정보. 핵심 도메인

    public static Membership generateMember (
            MembershipId membershipId
            , MembershipName membershipName
            , MembershipEmail membershipEmail
            , MembershipAddress membershipAddress
            , MembershipValid membershipValid
            , MembershipCorp membershipCorp
            , MembershipRefreshToken membershipRefreshToken
    ){
        return new Membership(
                membershipId.membershipId,
                membershipName.nameValue,
                membershipEmail.emailValue,
                membershipAddress.addressValue,
                membershipValid.validValue,
                membershipCorp.corpValue,
                membershipRefreshToken.refreshToken
        );
    }

    @Value
    public static class MembershipId {
        String membershipId;
    }

    @Value
    public static class MembershipName {
        String nameValue;
    }
    @Value
    public static class MembershipEmail {
        String emailValue;
    }

    @Value
    public static class MembershipAddress {
        String addressValue;
    }

    @Value
    public static class MembershipValid {
        boolean validValue;
    }

    @Value
    public static class MembershipCorp {
        boolean corpValue;
    }

    @Value
    public static class MembershipRefreshToken {
        String refreshToken;
    }
}
