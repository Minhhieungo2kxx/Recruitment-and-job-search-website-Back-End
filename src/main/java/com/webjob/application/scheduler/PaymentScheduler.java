package com.webjob.application.scheduler;

import com.webjob.application.enums.PaymentStatus;
import com.webjob.application.models.Entity.Payment;
import com.webjob.application.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentScheduler {
    private final PaymentRepository paymentRepository;

    /**
     * Tự động cập nhật thanh toán hết hạn sau mỗi 2 phút.
     */
    @Scheduled(fixedDelay = 120000)
    @Transactional
    public void expirePendingPayments() {
        List<Payment> expiredPayments =
                paymentRepository.findByStatusAndExpiredAtBefore(
                        PaymentStatus.PENDING.name(),
                        LocalDateTime.now()
                );

        expiredPayments.forEach(payment ->
                payment.setStatus(PaymentStatus.EXPIRED.name())
        );

        paymentRepository.saveAll(expiredPayments);

        if (!expiredPayments.isEmpty()) {
            log.info(
                    "Expired {} pending payments",
                    expiredPayments.size()
            );
        }
    }

    /**
     * Xóa các thanh toán cũ vào lúc 3 giờ sáng mỗi ngày.
     */
    @Scheduled(cron = "0 0 3 * * ?")
    @Transactional
    public void cleanupOldPayments() {
        int deleted = paymentRepository.deleteOldPayments();

        log.info("Deleted {} old payments", deleted);
    }




}
