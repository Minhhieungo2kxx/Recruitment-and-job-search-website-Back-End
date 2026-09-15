package com.webjob.application.service.Redis;

import com.github.benmanes.caffeine.cache.Cache;
import com.webjob.application.dto.record.JobRecommendationResponse;
import com.webjob.application.pubsub.dto.RecommendationCacheInvalidation;
import com.webjob.application.pubsub.publisher.RedisPublisher;
import lombok.RequiredArgsConstructor;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class RecommendationCacheService {
    private final Cache<String, JobRecommendationResponse> recommendationLocalCache;

    private final RedisTemplate<String, JobRecommendationResponse> recommendationRedisTemplate;

    private final RedisTemplate<String, Long> recommendationVersionRedisTemplate;

    private final RedissonClient redissonClient;

    private static final Duration CACHE_TTL = Duration.ofHours(24);


    private static final String RECOMMENDATION_CACHE_PREFIX =
            "recommendation:cache:user:";

    private static final String RECOMMENDATION_LOCK_PREFIX =
            "recommendation:lock:user:";

    public static final String RECOMMENDATION_INVALIDATION_CHANNEL =
            "recommendation.invalidate";

    private static final String RECOMMENDATION_VERSION_PREFIX =
            "recommendation:version:user:";

    private final RedisPublisher redisPublisher;


    private String recommendationCacheKey(Long userId) {
        return RECOMMENDATION_CACHE_PREFIX + userId;
    }

    private String recommendationLockKey(Long userId) {
        return RECOMMENDATION_LOCK_PREFIX + userId;
    }
    public JobRecommendationResponse get(Long userId) {

        String key = recommendationCacheKey(userId);

        // L1
        JobRecommendationResponse local = recommendationLocalCache.getIfPresent(key);

        if (local != null) {
            return local;
        }
        // L2
        JobRecommendationResponse redis = recommendationRedisTemplate.opsForValue().get(key);

        if (redis != null) {
            recommendationLocalCache.put(key, redis);
            return redis;
        }

        return null;
    }
    public void put(Long userId, JobRecommendationResponse response) {
        String key = recommendationCacheKey(userId);
        String versionKey = recommendationVersionKey(userId);

        // L2
        recommendationRedisTemplate.opsForValue().set(key, response, CACHE_TTL);

        // L1
        recommendationLocalCache.put(key, response);

//        Đánh dấu cache đã được generate mới
        recommendationRedisTemplate.opsForValue()
                .increment(versionKey);
    }
    public void invalidate(Long userId) {

        String key = recommendationCacheKey(userId);

        // Current instance
        recommendationLocalCache.invalidate(key);

        // Shared cache
        recommendationRedisTemplate.delete(key);

        // Other instances
        redisPublisher.publish(
                RECOMMENDATION_INVALIDATION_CHANNEL,
                RecommendationCacheInvalidation.builder()
                        .userId(userId)
                        .build()
        );

    }
    private String recommendationVersionKey(Long userId) {
        return RECOMMENDATION_VERSION_PREFIX + userId;
    }
    public Long getVersion(Long userId) {
        String versionKey = recommendationVersionKey(userId);

        return recommendationVersionRedisTemplate
                .opsForValue()
                .get(versionKey);
    }

    public JobRecommendationResponse getLatest(Long userId) {
        String key = recommendationCacheKey(userId);

        JobRecommendationResponse response =
                recommendationRedisTemplate.opsForValue().get(key);

        if (response != null) {
            recommendationLocalCache.put(key, response);
        }

        return response;
    }

    public RLock getLock(Long userId) {
        return redissonClient.getLock(RECOMMENDATION_LOCK_PREFIX + userId);
    }

}
