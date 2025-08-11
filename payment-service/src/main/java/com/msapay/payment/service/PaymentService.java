package com.msapay.payment.service;

import com.msapay.payment.controller.command.FinishSettlementCommand;
import com.msapay.payment.controller.command.RequestPaymentCommand;
import com.msapay.payment.service.usecase.RequestPaymentUseCase;
import com.msapay.payment.service.port.CreatePaymentPort;
import com.msapay.payment.service.port.GetMembershipPort;
import com.msapay.payment.service.port.GetRegisteredBankAccountPort;
import com.msapay.payment.domain.Payment;
import com.msapay.common.UseCase;
import lombok.RequiredArgsConstructor;

import javax.transaction.Transactional;
import java.util.List;

@UseCase
@RequiredArgsConstructor
@Transactional
public class PaymentService implements RequestPaymentUseCase {

    private final CreatePaymentPort createPaymentPort;

    private final GetMembershipPort getMembershipPort;
    private final GetRegisteredBankAccountPort getRegisteredBankAccountPort;

    // Todo Money Service -> Member Money 정보를 가져오기 위한 Port

    @Override
    public Payment requestPayment(RequestPaymentCommand command) {

        // 충전도, 멤버십, 머니 유효성 확인.....
        // getMembershipPort.getMembership(command.getRequestMembershipId());

        //getRegisteredBankAccountPort.getRegisteredBankAccount(command.getRequestMembershipId());

        //....

        // createPaymentPort
        return createPaymentPort.createPayment(
                command.getRequestMembershipId(),
                command.getRequestPrice(),
                command.getFranchiseId(),
                command.getFranchiseFeeRate());
    }

    @Override
    public List<Payment> getNormalStatusPayments() {
        return createPaymentPort.getNormalStatusPayments();
    }

    @Override
    public void finishPayment(FinishSettlementCommand command) {
        createPaymentPort.changePaymentRequestStatus(command.getPaymentId(), 2);
    }
}
