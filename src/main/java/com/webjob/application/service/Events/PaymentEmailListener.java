package com.webjob.application.service.Events;

import com.webjob.application.dto.Request.PaymentSuccessDto;
import com.webjob.application.dto.record.PaymentSuccessEvent;
import com.webjob.application.models.Entity.Payment;
import com.webjob.application.repository.PaymentRepository;
import com.webjob.application.service.SendEmail.ApplicationEmailService;
import com.webjob.application.utils.UtilFormat;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor

public class PaymentEmailListener {
    private final PaymentRepository paymentRepository;
    private final ApplicationEmailService emailAsyncService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handlePaymentSuccess(PaymentSuccessEvent event) {

        Payment payment = paymentRepository.findById(event.paymentId())
                .orElseThrow();
        PaymentSuccessDto dto = PaymentSuccessDto.builder()
                .email(payment.getUser().getEmail())
                .userName(payment.getUser().getFullName())
                .jobName(payment.getJob().getName())
                .amount(UtilFormat.formatAmount(payment.getAmount()))
                .status(payment.getStatus())
                .transactionId(payment.getTransactionId())
                .payDate(UtilFormat.formatTime(payment.getPayDate()))
                .gateway(payment.getProvider())
                .bankCode(payment.getBankCode())
                .responseCode(payment.getResponseCode())
                .orderInfo(payment.getOrderInfo())
                .payType(payment.getPayType())
                .orderType(payment.getOrderType())
                .build();
        emailAsyncService.sendMomoPaymentEmail(dto);
    }

}
