package com.webjob.application.pubsub.publisher;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

@Component
@RequiredArgsConstructor
public class RedisPublisher {

    private final RedisTemplate<String,Object> redisTemplate;

    @Retryable(
            retryFor = RedisConnectionFailureException.class,
            maxAttempts = 3,
            backoff = @Backoff(delay = 500, multiplier = 2)
    )
    public void publish(String channel,Object data){
        redisTemplate.convertAndSend(channel,data);

    }
}
