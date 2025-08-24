package com.msapay.membership.persistence;

import lombok.*;
import javax.persistence.*;

@Entity
@Table(name = "membership")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = lombok.AccessLevel.PROTECTED)
@Builder
public class MembershipJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long membershipId;

    private String name;
    private String address;
    private String email;
    private boolean valid;
    private boolean corp;
    private String refreshToken;

    public static MembershipJpaEntity of(String name, String address, String email, boolean valid, boolean corp, String refreshToken) {
        return MembershipJpaEntity.builder()
                .membershipId(null) // 신규 생성이므로 PK는 null로 지정
                .name(name)
                .address(address)
                .email(email)
                .valid(valid)
                .corp(corp)
                .refreshToken(refreshToken)
                .build();
    }

    @Override
    public String toString() {
        return "MembershipJpaEntity{" +
                "membershipId=" + membershipId +
                ", name='" + name + '\'' +
                ", address='" + address + '\'' +
                ", email='" + email + '\'' +
                ", valid=" + valid +
                ", corp=" + corp +
                ", refreshToken='" + refreshToken + '\'' +
                '}';
    }
}
