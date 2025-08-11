package com.msapay.remittance.outbound.membership;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class Membership {
    private String membershipId;
    private  String name;
    private  String email;
    private  String address;
    private  boolean isValid;
    private  boolean isCorp;

    @Override
    public String toString() {
        return "Membership from Remittance {" +
                "membershipId='" + membershipId + '\'' +
                ", name='" + name + '\'' +
                ", email='" + email + '\'' +
                ", address='" + address + '\'' +
                ", isValid=" + isValid +
                ", isCorp=" + isCorp +
                '}';
    }
}
