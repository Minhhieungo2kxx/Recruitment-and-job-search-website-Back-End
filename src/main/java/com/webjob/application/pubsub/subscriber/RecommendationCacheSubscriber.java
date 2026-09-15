package com.webjob.application.pubsub.subscriber;

import com.github.benmanes.caffeine.cache.Cache;
import com.webjob.application.dto.record.JobRecommendationResponse;
import com.webjob.application.pubsub.dto.RecommendationCacheInvalidation;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RecommendationCacheSubscriber {
    private final Cache<String, JobRecommendationResponse>
            recommendationLocalCache;
    private static final String RECOMMENDATION_CACHE_PREFIX =
            "recommendation:cache:user:";

    public void receiveMessage(RecommendationCacheInvalidation message) {
        String key = RECOMMENDATION_CACHE_PREFIX + message.getUserId();
        recommendationLocalCache.invalidate(key);
    }
}
