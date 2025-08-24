package com.msapay.membership.persistence;

import com.msapay.common.PersistenceAdapter;
import com.msapay.membership.outbound.vault.VaultAdapter;
import com.msapay.membership.service.port.FindMembershipPort;
import com.msapay.membership.service.port.ModifyMembershipPort;
import com.msapay.membership.service.port.RegisterMembershipPort;
import com.msapay.membership.domain.Membership;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;



@PersistenceAdapter
@RequiredArgsConstructor
public class MembershipPersistenceAdapter implements RegisterMembershipPort, FindMembershipPort, ModifyMembershipPort {
    private final SpringDataMembershipRepository membershipRepository;
    @Autowired(required = false)
    private VaultAdapter vaultAdapter;

    private String encryptIfNeeded(String plainText) {
        return (vaultAdapter != null) ? vaultAdapter.encrypt(plainText) : plainText;
    }
    private String decryptIfNeeded(String encryptedText) {
        return (vaultAdapter != null) ? vaultAdapter.decrypt(encryptedText) : encryptedText;
    }

    @Override
    public MembershipDto createMembership(Membership.MembershipName membershipName, Membership.MembershipEmail membershipEmail, Membership.MembershipAddress membershipAddress, Membership.MembershipValid membershipValid, Membership.MembershipCorp membershipCorp) {
        String encryptedAddress = encryptIfNeeded(membershipAddress.getAddressValue());
        String encryptedEmail = encryptIfNeeded(membershipEmail.getEmailValue());
        MembershipJpaEntity jpaEntity = MembershipJpaEntity.of(
                membershipName.getNameValue(),
                encryptedAddress,
                encryptedEmail,
                membershipValid.isValidValue(),
                membershipCorp.isCorpValue(),
                ""
        );
        membershipRepository.save(jpaEntity);

        return MembershipDto.of(
                jpaEntity.getMembershipId(),
                membershipName.getNameValue(),
                membershipAddress.getAddressValue(),
                membershipEmail.getEmailValue(),
                membershipValid.isValidValue(),
                membershipCorp.isCorpValue(),
                ""
        );
    }

    @Override
    public MembershipDto findMembership(Membership.MembershipId membershipId) {
        MembershipJpaEntity membershipJpaEntity = membershipRepository.getById(Long.parseLong(membershipId.getMembershipId()));
        String encryptedAddressString = membershipJpaEntity.getAddress();
        String decryptedAddressString = decryptIfNeeded(encryptedAddressString);
        String encryptedEmailString = membershipJpaEntity.getEmail();
        String decryptedEmailString = decryptIfNeeded(encryptedEmailString);
        return MembershipDto.of(
                membershipJpaEntity.getMembershipId(),
                membershipJpaEntity.getName(),
                decryptedAddressString,
                decryptedEmailString,
                membershipJpaEntity.isValid(),
                membershipJpaEntity.isCorp(),
                membershipJpaEntity.getRefreshToken()
        );
    }

    @Override
    public List<MembershipDto> findMembershipListByAddress(Membership.MembershipAddress membershipAddress) {
        // 관악구, 서초구, 강남구 중 하나
        String address = membershipAddress.getAddressValue();
        List<MembershipJpaEntity> membershipJpaEntityList = membershipRepository.findByAddress(address);
        List<MembershipDto> membershipDtoList = new ArrayList<>();

        for (MembershipJpaEntity membershipJpaEntity : membershipJpaEntityList) {
            String encryptedAddressString = membershipJpaEntity.getAddress();
            String decryptedAddressString = decryptIfNeeded(encryptedAddressString);
            String encryptedEmailString = membershipJpaEntity.getEmail();
            String decryptedEmailString = decryptIfNeeded(encryptedEmailString);
            membershipDtoList.add(MembershipDto.of(
                    membershipJpaEntity.getMembershipId(),
                    membershipJpaEntity.getName(),
                    decryptedAddressString,
                    decryptedEmailString,
                    membershipJpaEntity.isValid(),
                    membershipJpaEntity.isCorp(),
                    membershipJpaEntity.getRefreshToken()
            ));
        }
        return membershipDtoList;
    }

    @Override
    public MembershipDto modifyMembership(Membership.MembershipId membershipId, Membership.MembershipName membershipName, Membership.MembershipEmail membershipEmail, Membership.MembershipAddress membershipAddress, Membership.MembershipValid membershipValid, Membership.MembershipCorp membershipCorp, Membership.MembershipRefreshToken membershipRefreshToken) {
        MembershipJpaEntity entity = membershipRepository.getById(Long.parseLong(membershipId.getMembershipId()));

        String encryptedAddress = encryptIfNeeded(membershipAddress.getAddressValue());
        String encryptedEmail = encryptIfNeeded(membershipEmail.getEmailValue());
        entity.setName(membershipName.getNameValue());
        entity.setAddress(encryptedAddress);
        entity.setEmail(encryptedEmail);
        entity.setCorp(membershipCorp.isCorpValue());
        entity.setValid(membershipValid.isValidValue());
        entity.setRefreshToken(membershipRefreshToken.getRefreshToken());
        membershipRepository.save(entity);

        // Todo 리턴 전에 새로운 객체로 평문화된 멤버 정보를 리턴해 줘야 해요.
        return MembershipDto.of(
                entity.getMembershipId(),
                membershipName.getNameValue(),
                membershipAddress.getAddressValue(),
                membershipEmail.getEmailValue(),
                membershipValid.isValidValue(),
                membershipCorp.isCorpValue(),
                membershipRefreshToken.getRefreshToken()
        );
    }
}
