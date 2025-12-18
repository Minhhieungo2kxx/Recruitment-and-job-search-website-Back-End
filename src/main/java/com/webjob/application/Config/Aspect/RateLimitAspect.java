package com.webjob.application.Config.Aspect;

import com.webjob.application.Annotation.RateLimit;

import com.webjob.application.Exception.Customs.TooManyRequestsException;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.Duration;

@Aspect
@Component
public class RateLimitAspect {
    private final RedisTemplate<String, Object> redisTemplate;
    private static final Logger logger = LoggerFactory.getLogger(RateLimitAspect.class);

    public RateLimitAspect(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Around("@annotation(rateLimit)")
    public Object rateLimit(ProceedingJoinPoint joinPoint, RateLimit rateLimit) throws Throwable {
        ServletRequestAttributes sra = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        HttpServletRequest request = sra.getRequest();
        String endpoint = request.getRequestURI(); // Thêm endpoint để phân biệt
        String method = request.getMethod();

        String key;
        String blacklistKey;
        if ("TOKEN".equalsIgnoreCase(rateLimit.keyType())) {
            String userId =extractUserIdFromContext();
            key = "RATE_LIMIT:TOKEN:" + userId + ":" + method + ":" + endpoint;
            blacklistKey = "BLACKLIST:TOKEN:" + userId + ":" + method + ":" + endpoint;
        } else {
            String ip = getClientIP(request);
            key = "RATE_LIMIT:IP:" + ip + ":" + method + ":" + endpoint;
            blacklistKey = "BLACKLIST:IP:" + ip + ":" + method + ":" + endpoint;
        }


        // Kiểm tra xem IP có đang bị block không
        if (Boolean.TRUE.equals(redisTemplate.hasKey(blacklistKey))) {
            logger.warn("Blocked IP {} tried to access while still blocked", blacklistKey);
            throw new TooManyRequestsException("IP temporarily blocked due to abuse");
        }

        ValueOperations<String, Object> ops = redisTemplate.opsForValue();
        Integer count = (Integer) ops.get(key);
        int max = rateLimit.maxRequests();

        if (count == null) {
            ops.set(key, 1, Duration.ofSeconds(rateLimit.timeWindowSeconds()));
        } else if (count < max) {
            ops.increment(key);
        } else {
            // Nếu quá giới hạn, block IP tạm thời 5 phút
            redisTemplate.opsForValue().set(blacklistKey, "BLOCKED", Duration.ofMinutes(5));
            logger.warn("IP {} has been blocked due to too many requests", blacklistKey);
            throw new TooManyRequestsException("Too many requests. IP temporarily blocked.");
        }

        return joinPoint.proceed();
    }

private String extractUserIdFromContext() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication == null || !authentication.isAuthenticated()) {
        return "anonymous";
    }

    String userId = authentication.getName();
    return userId != null ? userId : "anonymous";


}


    private String getClientIP(HttpServletRequest request) {
        String xfHeader = request.getHeader("X-Forwarded-For");
        return xfHeader == null ? request.getRemoteAddr() : xfHeader.split(",")[0];
    }
}
