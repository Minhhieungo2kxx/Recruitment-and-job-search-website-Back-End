package com.webjob.application.service.Redis;

import com.github.benmanes.caffeine.cache.Cache;
import com.webjob.application.dto.Request.Redis.PermissionSet;
import com.webjob.application.models.Entity.User;
import com.webjob.application.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class PermissionCacheService {
    private final Cache<String, PermissionSet> localCache;
    private final RedisTemplate<String, PermissionSet> redisTemplate;
    private final UserRepository userRepository;

    private static final String PREFIX = "USER_PERMISSION:";

    public PermissionSet getPermissions(String userId) {

        PermissionSet local = localCache.getIfPresent(userId);

        if (local != null) {
            return local;
        }

        String key = PREFIX + userId;

        PermissionSet redis = redisTemplate.opsForValue().get(key);

        if (redis != null) {
            localCache.put(userId, redis);
            return redis;
        }

        PermissionSet db = loadFromDatabase(userId);

        redisTemplate.opsForValue()
                .set(key, db, Duration.ofMinutes(30));

        localCache.put(userId, db);

        return db;
    }

    private PermissionSet loadFromDatabase(String userId) {

        User user = userRepository.findById(Long.valueOf(userId))
                .orElseThrow(() -> new UsernameNotFoundException("User not found with id: " + userId));

        if (user == null || user.getRole() == null) {
            return new PermissionSet();
        }

        PermissionSet permissionSet = new PermissionSet();

        user.getRole()
                .getPermissions()
                .forEach(p ->
                        permissionSet.add(p.getMethod(), p.getApiPath())
                );

        return permissionSet;
    }

    public void evict(String userId) {

        localCache.invalidate(userId);
        redisTemplate.delete(PREFIX + userId);
    }



}
