package com.msapay.membership.controller.command;

import com.msapay.common.SelfValidating;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.AssertTrue;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@Builder
@Data
@EqualsAndHashCode(callSuper = false)
public class RegisterMembershipCommand extends SelfValidating<RegisterMembershipCommand> {

    @NotNull
    private final String name;

    @NotNull
    private final String email;

    @NotNull
    @NotBlank
    private final String address;

    @AssertTrue
    private final boolean valid;

    private final boolean corp;

    public RegisterMembershipCommand(String name, String email, String address, boolean valid, boolean corp) {
        this.name = name;
        this.email = email;
        this.address = address;
        this.valid = valid;
        this.corp = corp;

        this.validateSelf();
    }
}
