package com.webjob.application.Service.Redis;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
public class RedisLockService {
    private final RedisTemplate<String, String> redisTemplate;

    public RedisLockService(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public boolean tryLock(String key, String value, long timeout, TimeUnit unit) {
        Boolean success = redisTemplate.opsForValue()
                .setIfAbsent(key, value, timeout, unit);
        return Boolean.TRUE.equals(success);
    }
    public void unlock(String key, String value) {
        String currentValue = redisTemplate.opsForValue().get(key);
        if (value.equals(currentValue)) {
            redisTemplate.delete(key);
        }
    }


}
