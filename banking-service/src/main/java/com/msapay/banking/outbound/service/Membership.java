package com.msapay.banking.outbound.service;

import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Membership {
    private String membershipId;
    private String name;
    private String email;
    private String address;
    private boolean valid;
    private boolean corp;
    private String refreshToken;
}
