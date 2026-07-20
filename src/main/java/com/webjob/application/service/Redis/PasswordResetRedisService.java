package com.webjob.application.service.Redis;

import com.webjob.application.exception.Customs.RedisUnavailableException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
@Slf4j
public class PasswordResetRedisService {
    private final RedisTemplate<String, Object> redisTemplate;

    private static final String TOKEN_PREFIX = "password-reset:";
    private static final String USER_PREFIX = "password-reset-user:";

    public void save(Long userId, String token, Duration ttl) {
        try {
            String oldToken = getToken(userId);

            if (oldToken != null) {
                redisTemplate.delete(TOKEN_PREFIX + oldToken);
            }

            redisTemplate.opsForValue().set(
                    TOKEN_PREFIX + token,
                    userId,
                    ttl
            );

            redisTemplate.opsForValue().set(
                    USER_PREFIX + userId,
                    token,
                    ttl
            );

        }catch (RedisConnectionFailureException ex){
            log.error("Cannot connect to Redis", ex);
            throw new RedisUnavailableException(ex.getMessage());

        }



    }

    public Long getUserId(String token) {
        try {
            Object value = redisTemplate.opsForValue().get(TOKEN_PREFIX + token);

            if (value == null)
                return null;

            return Long.valueOf(value.toString());

        }catch (RedisConnectionFailureException ex){
            log.error("Cannot connect to Redis", ex);
            throw new RedisUnavailableException(ex.getMessage());

        }


    }

    public String getToken(Long userId) {

        Object value = redisTemplate.opsForValue().get(USER_PREFIX + userId);

        return value == null ? null : value.toString();
    }

    public void delete(Long userId, String token) {
        try {
            redisTemplate.delete(TOKEN_PREFIX + token);

            redisTemplate.delete(USER_PREFIX + userId);
        }
        catch (RedisConnectionFailureException ex) {
            log.error("Cannot connect to Redis", ex);

            throw new RedisUnavailableException(ex.getMessage());
        }


    }
}
