package com.msapay.money.service.usecase;


import com.msapay.money.domain.MemberMoney;
import com.msapay.money.controller.request.MoneyChangingRequest;
import com.msapay.money.controller.command.FindMemberMoneyListByMembershipIdsCommand;
import com.msapay.money.controller.command.IncreaseMoneyRequestCommand;

import java.util.List;

public interface IncreaseMoneyRequestUseCase {
    MoneyChangingRequest increaseMoneyRequest(IncreaseMoneyRequestCommand command);
    MoneyChangingRequest increaseMoneyRequestAsync(IncreaseMoneyRequestCommand command);
    MoneyChangingRequest increaseMoneyRequestByEvent(IncreaseMoneyRequestCommand command);

    List<MemberMoney> findMemberMoneyListByMembershipIds(FindMemberMoneyListByMembershipIdsCommand command);
}
