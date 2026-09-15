package com.webjob.application.service.Redis;

import com.github.benmanes.caffeine.cache.Cache;
import com.webjob.application.dto.Request.Redis.PermissionSet;
import com.webjob.application.dto.record.PermissionCacheInvalidationEvent;
import com.webjob.application.exception.Customs.BusinessException;
import com.webjob.application.models.Entity.Role;
import com.webjob.application.models.Entity.RolePermission;
import com.webjob.application.models.Entity.User;
import com.webjob.application.pubsub.publisher.RedisPublisher;
import com.webjob.application.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class PermissionCacheService {
    private static final String USER_PERMISSION_KEY_PREFIX = "USER_PERMISSION:";
    private static final String INVALIDATION_CHANNEL = "permission.invalidate";

    private static final String LOCK_PREFIX = "LOCK:USER_PERMISSION:";

    private final Cache<String, PermissionSet> localCache;
    private final RedisTemplate<String, PermissionSet> redisTemplate;
    private final UserRepository userRepository;
    private final RedisPublisher redisPublisher;

    private final RedissonClient redissonClient;

    public PermissionSet getPermissions(Long userId) {

        String key = buildUserPermissionKey(userId);


        // L1 - CAFFEINE
        PermissionSet local = localCache.getIfPresent(key);
        if (local != null) {
            return local;
        }

        // L2 - REDIS
        PermissionSet redis = redisTemplate.opsForValue().get(key);
        if (redis != null) {
            localCache.put(key, redis);
            return redis;
        }

        // DISTRIBUTED LOCK
        RLock lock = getLock(userId);

        boolean acquired = false;
        try {

//       Không truyền leaseTime để Redisson, watchdog tự quản lý lock.

            acquired = lock.tryLock(30, TimeUnit.SECONDS);
            if (!acquired) {
                throw new BusinessException("Hệ thống đang tải quyền, " + "vui lòng thử lại sau");
            }

            // DOUBLE CHECK L1
            PermissionSet localAfterLock = localCache.getIfPresent(key);
            if (localAfterLock != null) {
                return localAfterLock;
            }

            // DOUBLE CHECK L2

            PermissionSet redisAfterLock = redisTemplate.opsForValue().get(key);
            if (redisAfterLock != null) {
                localCache.put(key, redisAfterLock);
                return redisAfterLock;
            }

            // LOAD DATABASE
            PermissionSet db = loadFromDatabase(userId);

            // CACHE

            redisTemplate.opsForValue().set(key, db, Duration.ofHours(1));
            localCache.put(key, db);
            return db;

        } catch (InterruptedException e) {

            Thread.currentThread().interrupt();

            throw new BusinessException("Không thể xử lý permission");

        } finally {

            if (acquired && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }

    }

    private PermissionSet loadFromDatabase(Long userId) {

        User user = userRepository.findActiveRoleUser(userId)
                .orElseThrow(() -> new UsernameNotFoundException("User not found or role inactive"));

        Role role = user.getRole();

        if (role == null || !role.isActive()) {
            return new PermissionSet();
        }

        PermissionSet permissionSet = new PermissionSet();

        role.getRolePermissions()
                .stream()
                .map(RolePermission::getPermission)
                .filter(Objects::nonNull)
                .forEach(permission ->
                        permissionSet.add(
                                permission.getMethod(),
                                permission.getApiPath()
                        )
                );

        return permissionSet;
    }

    public void evict(Long userId) {

        String key = buildUserPermissionKey(userId);

        localCache.invalidate(key);
        redisTemplate.delete(key);

    }
    public void publishInvalidation(List<Long> userIds) {
        redisPublisher.publish(
                INVALIDATION_CHANNEL,
                new PermissionCacheInvalidationEvent(userIds)
        );
    }

    private String buildUserPermissionKey(Long userId) {
        return USER_PERMISSION_KEY_PREFIX + userId;
    }


    private RLock getLock(Long userId) {

        return redissonClient.getLock(
                LOCK_PREFIX + userId
        );
    }

}
