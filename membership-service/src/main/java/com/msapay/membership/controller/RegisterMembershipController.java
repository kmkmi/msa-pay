package com.msapay.membership.controller;

import com.msapay.common.WebAdapter;
import com.msapay.membership.controller.command.RegisterMembershipCommand;
import com.msapay.membership.service.usecase.RegisterMembershipUseCase;
import com.msapay.membership.controller.request.RegisterMembershipRequest;
import com.msapay.membership.domain.Membership;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;


@WebAdapter
@RestController
@RequiredArgsConstructor
public class RegisterMembershipController {

    private final RegisterMembershipUseCase registerMembershipUseCase;
    @PostMapping(path = "/membership/register")
    Membership registerMembership(@RequestBody RegisterMembershipRequest request) {

        RegisterMembershipCommand command = RegisterMembershipCommand.builder()
                .name(request.getName())
                .address(request.getAddress())
                .email(request.getEmail())
                .valid(true)
                .corp(request.isCorp())
                .build();

        return registerMembershipUseCase.registerMembership(command);
    }
}
