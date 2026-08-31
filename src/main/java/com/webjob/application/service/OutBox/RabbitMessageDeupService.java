package com.webjob.application.service.OutBox;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.TimeUnit;


@Service
@RequiredArgsConstructor
@Slf4j
public class RabbitMessageDeupService {
    private final RedissonClient redissonClient;
    private static final String KEY_PREFIX = "rabbitmq:dedup:";

    private static final long PROCESSING_TTL_MINUTES = 5;
    private static final long PROCESSED_TTL_MINUTES = 10;

    public boolean tryStartProcessing(
            String queueName,
            String eventId
    ) {
        String key = buildKey(queueName, eventId);
        RBucket<String> bucket = redissonClient.getBucket(key);

        boolean acquired = bucket.setIfAbsent(
                "PROCESSING",
                Duration.ofMinutes(PROCESSING_TTL_MINUTES)
        );

        if (acquired) {
            log.debug("Acquired processing lock for queue: {}, eventId: {}", queueName, eventId);
        } else {
            log.warn("Failed to acquire processing lock (already in progress or processed) for queue: {}, eventId: {}", queueName, eventId);
        }

        return acquired;
    }

    public void markProcessed(
            String queueName,
            String eventId
    ) {

        String key = buildKey(queueName, eventId);

        RBucket<String> bucket = redissonClient.getBucket(key);

        bucket.set("PROCESSED", PROCESSED_TTL_MINUTES, TimeUnit.MINUTES);
        log.debug("Marked event as PROCESSED in Redis for queue: {}, eventId: {}", queueName, eventId);
    }

    private String buildKey(
            String queueName,
            String eventId
    ) {

        return KEY_PREFIX
                + queueName
                + ":"
                + eventId;
    }

    public void removeProcessing(
            String queueName,
            String eventId
    ) {
        String key = buildKey(queueName, eventId);
        RBucket<String> bucket = redissonClient.getBucket(key);

        boolean deleted = bucket.delete();
        if (deleted) {
            log.debug("Removed processing lock/state for queue: {}, eventId: {}", queueName, eventId);
        }
    }


}
