package com.msapay.payment.service.usecase;

import com.msapay.payment.controller.command.FinishSettlementCommand;
import com.msapay.payment.controller.command.RequestPaymentCommand;
import com.msapay.payment.domain.Payment;

import java.util.List;

public interface RequestPaymentUseCase {
    Payment requestPayment(RequestPaymentCommand command);

    // 원래대로라면,, command . start date, end date
    List<Payment> getNormalStatusPayments();

    void finishPayment(FinishSettlementCommand command);
}
