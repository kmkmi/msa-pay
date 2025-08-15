package com.msapay.membership.persistence;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MembershipDto {
    private Long membershipId;
    private String name;
    private String address;
    private String email;
    private boolean valid;
    private boolean corp;
    private String refreshToken;

    public static MembershipDto of(Long membershipId, String name, String address, String email, boolean valid, boolean corp, String refreshToken) {
        return MembershipDto.builder()
                .membershipId(membershipId)
                .name(name)
                .address(address)
                .email(email)
                .valid(valid)
                .corp(corp)
                .refreshToken(refreshToken)
                .build();
    }
}