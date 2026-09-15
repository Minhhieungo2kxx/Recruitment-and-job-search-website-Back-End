package com.webjob.application.pubsub.subscriber;


import com.github.benmanes.caffeine.cache.Cache;
import com.webjob.application.dto.Request.Redis.PermissionSet;
import com.webjob.application.dto.record.PermissionCacheInvalidationEvent;
import com.webjob.application.service.Redis.PermissionCacheService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PermissionCacheSubscriber {
    private final Cache<String, PermissionSet> localCache;
    private static final String USER_PERMISSION_KEY_PREFIX = "USER_PERMISSION:";

    public void receiveMessage(PermissionCacheInvalidationEvent event) {
        if(!event.userIds().isEmpty()){
           for(Long userID: event.userIds()){
               localCache.invalidate(buildUserPermissionKey(userID));
           }
        }

    }
    private String buildUserPermissionKey(Long userId) {
        return USER_PERMISSION_KEY_PREFIX + userId;
    }

}
