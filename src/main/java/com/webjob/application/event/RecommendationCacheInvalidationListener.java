package com.webjob.application.event;


import com.webjob.application.dto.record.RecommendationCacheInvalidateEvent;
import com.webjob.application.repository.JobRecommendationRepository;
import com.webjob.application.service.Redis.RecommendationCacheService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.Duration;
import java.time.Instant;

@Component
@RequiredArgsConstructor
@Slf4j
public class RecommendationCacheInvalidationListener {
    private final JobRecommendationRepository recommendationRepository;

    private final RecommendationCacheService recommendationCacheService;

    @Async("taskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onInvalidate(RecommendationCacheInvalidateEvent event) {
        Instant now = Instant.now();

        if ("saved_job".equals(event.reason())) {
            int updated = recommendationRepository.invalidateByUserIdIfOlderThan(
                    event.userId(),now,now.minus(Duration.ofHours(4)));
            if (updated > 0) {
                log.info("Invalidated {} recommendation cache(s) for userId={} reason={}",
                    updated, event.userId(), event.reason());
                recommendationCacheService.invalidate(event.userId());
            }
        } else {
            int updated = recommendationRepository.invalidateByUserId(event.userId(), now);
            if (updated > 0) {
                log.info("Invalidated {} recommendation cache(s) for userId={} reason={}",
                        updated, event.userId(), event.reason());
                recommendationCacheService.invalidate(event.userId());
            }
        }


    }

}
