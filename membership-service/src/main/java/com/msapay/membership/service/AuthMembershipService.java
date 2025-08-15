package com.msapay.membership.service;

import com.msapay.common.UseCase;
import com.msapay.membership.persistence.MembershipDto;
import com.msapay.membership.persistence.MembershipJpaEntity;
import com.msapay.membership.persistence.MembershipMapper;
import com.msapay.membership.service.usecase.AuthMembershipUseCase;
import com.msapay.membership.controller.command.LoginMembershipCommand;
import com.msapay.membership.controller.command.RefreshTokenCommand;
import com.msapay.membership.controller.command.ValidateTokenCommand;
import com.msapay.membership.service.port.AuthMembershipPort;
import com.msapay.membership.service.port.FindMembershipPort;
import com.msapay.membership.service.port.ModifyMembershipPort;
import com.msapay.membership.domain.JwtToken;
import com.msapay.membership.domain.Membership;
import lombok.RequiredArgsConstructor;

import javax.transaction.Transactional;

@RequiredArgsConstructor
@UseCase
@Transactional
public class AuthMembershipService implements AuthMembershipUseCase {
    private final AuthMembershipPort authMembershipPort;
    private final FindMembershipPort findMembershipPort;
    private final ModifyMembershipPort modifyMembershipPort;
    private final MembershipMapper mapper;
    @Override
    public JwtToken loginMembership(LoginMembershipCommand command) {

        String membershipId = command.getMembershipId();
        MembershipDto membershipDto = findMembershipPort.findMembership(
                new Membership.MembershipId(membershipId)
        );

        if (membershipDto.isValid()){
            String jwtToken = authMembershipPort.generateJwtToken(
                    new Membership.MembershipId(membershipId)
            );
            String refreshToken = authMembershipPort.generateRefreshToken(
                    new Membership.MembershipId(membershipId)
            );

            // membership jpa 내에, refresh token 을 저장한다.
            modifyMembershipPort.modifyMembership(
                    new Membership.MembershipId(membershipId),
                    new Membership.MembershipName(membershipDto.getName()),
                    new Membership.MembershipEmail(membershipDto.getEmail()),
                    new Membership.MembershipAddress(membershipDto.getAddress()),
                    new Membership.MembershipValid(membershipDto.isValid()),
                    new Membership.MembershipCorp(membershipDto.isCorp()),
                    new Membership.MembershipRefreshToken(refreshToken)
            );

            return JwtToken.generateJwtToken(
                    new JwtToken.MembershipId(membershipId),
                    new JwtToken.MembershipJwtToken(jwtToken),
                    new JwtToken.MembershipRefreshToken(refreshToken)
            );
        }

        return null;
    }

    @Override
    public JwtToken refreshJwtTokenByRefreshToken(RefreshTokenCommand command) {
        String requestedRefreshToken = command.getRefreshToken();
        boolean isValid = authMembershipPort.validateJwtToken(requestedRefreshToken);

        if(isValid) {
            Membership.MembershipId membershipId = authMembershipPort.parseMembershipIdFromToken(requestedRefreshToken);
            String membershipIdString = membershipId.getMembershipId();

            MembershipDto membershipDto = findMembershipPort.findMembership(membershipId);
            if(!membershipDto.getRefreshToken().equals(
                    command.getRefreshToken()
            )) {
                return null;
            }

            // 고객의 refresh token 정보와, 요청받은 refresh token 정보가 일치하는지 확인 된 상태.
            if (membershipDto.isValid()){
                String newJwtToken = authMembershipPort.generateJwtToken(
                        new Membership.MembershipId(membershipIdString)
                );

                return JwtToken.generateJwtToken(
                        new JwtToken.MembershipId(membershipIdString),
                        new JwtToken.MembershipJwtToken(newJwtToken),
                        new JwtToken.MembershipRefreshToken(requestedRefreshToken)
                );
            }
        }

        return null;
    }

    @Override
    public boolean validateJwtToken(ValidateTokenCommand command) {
        String jwtToken = command.getJwtToken();
        return authMembershipPort.validateJwtToken(jwtToken);
    }

    @Override
    public Membership getMembershipByJwtToken(ValidateTokenCommand command) {
        String jwtToken = command.getJwtToken();
        boolean isValid = authMembershipPort.validateJwtToken(jwtToken);

        if(isValid) {
            Membership.MembershipId membershipId = authMembershipPort.parseMembershipIdFromToken(jwtToken);
            String membershipIdString = membershipId.getMembershipId();

            MembershipDto membershipDto = findMembershipPort.findMembership(membershipId);
            if (!membershipDto.getRefreshToken().equals(
                    command.getJwtToken()
            )) {
                return null;
            }

            return mapper.mapToDto(membershipDto);
        }
        return null;
    }
}
