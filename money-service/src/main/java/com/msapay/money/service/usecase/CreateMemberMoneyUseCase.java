package com.msapay.money.service.usecase;

import com.msapay.money.controller.command.CreateMemberMoneyCommand;

public interface CreateMemberMoneyUseCase {

   void createMemberMoney (CreateMemberMoneyCommand command);
}
