package com.webjob.application.scheduler;

import com.webjob.application.service.OutBox.OutboxService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxRecoveryScheduler {
    private final OutboxService outboxService;

    @Scheduled(fixedDelay = 10000)
    public void recoverStuckEvents() {
        try {

            int recoveredCount = outboxService.recoverExpiredEvents();
            if (recoveredCount > 0) {
                log.info("Đã khôi phục thành công {} outbox event bị kẹt.", recoveredCount);
            }
        } catch (Exception e) {
            log.error("Lỗi xảy ra trong quá trình khôi phục outbox event bị kẹt: {}", e.getMessage(), e);
        }
    }
}
