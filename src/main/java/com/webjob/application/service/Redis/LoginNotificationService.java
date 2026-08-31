package com.webjob.application.service.Redis;

import lombok.RequiredArgsConstructor;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class LoginNotificationService {
    private final RedissonClient redissonClient;

    private static final String KEY_PREFIX = "login-notification:";

    private static final Duration NOTIFICATION_TTL = Duration.ofHours(24);

    public boolean shouldSendLoginNotification(Long userId) {

        String key = KEY_PREFIX + userId;

        RBucket<String> bucket = redissonClient.getBucket(key);
        return bucket.setIfAbsent(
                "1",
                NOTIFICATION_TTL
        );
    }
    public void removeNotificationFlag(Long userId) {

        String key = KEY_PREFIX + userId;

        redissonClient.getBucket(key).delete();
    }
}
