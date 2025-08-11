package com.msapay.money.service.port;

import com.msapay.money.persistence.MemberMoneyJpaEntity;
import com.msapay.money.domain.MemberMoney;

public interface GetMemberMoneyPort {
    MemberMoneyJpaEntity getMemberMoney(
            MemberMoney.MembershipId memberId
    );
}
