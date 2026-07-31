package com.webjob.application.pubsub.publisher;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RedisPublisher {

    private final RedisTemplate<String,Object> redisTemplate;

    public void publish(String channel,Object data){

        redisTemplate.convertAndSend(channel,data);

    }
}
