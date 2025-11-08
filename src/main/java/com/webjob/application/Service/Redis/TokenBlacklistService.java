package com.webjob.application.Service.Redis;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.TimeUnit;

@Service
public class TokenBlacklistService {
    private final RedisTemplate<String, Object> redisTemplate;
    private final JwtDecoder jwtDecoder;

    @Autowired
    public TokenBlacklistService(RedisTemplate<String, Object> redisTemplate, JwtDecoder jwtDecoder) {
        this.redisTemplate = redisTemplate;
        this.jwtDecoder = jwtDecoder;
    }

    private static final String BLACKLIST_PREFIX = "blacklist:";

    // Thêm token vào danh sách blacklist với TTL = thời gian còn lại
    public void blacklistToken(String token, long expirationSeconds) {
        String key = BLACKLIST_PREFIX + token;
        redisTemplate.opsForValue().set(key, "logout", expirationSeconds, TimeUnit.SECONDS);
    }

    // Kiểm tra token có bị blacklist không
    public boolean isTokenBlacklisted(String token) {
        String key = BLACKLIST_PREFIX + token;
        return redisTemplate.hasKey(key);
    }
    public String extractBearerToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            return header.substring(7);
        }
        return null;
    }
    public long getRemainingValidity(String token) {
        Jwt jwt = jwtDecoder.decode(token);
        Instant expiration = jwt.getExpiresAt();
        if (expiration == null) return 0;
        return Duration.between(Instant.now(), expiration).getSeconds();
    }

}
