package com.webjob.application.event;

import com.webjob.application.dto.record.PermissionCacheInvalidationEvent;
import com.webjob.application.pubsub.publisher.RedisPublisher;
import com.webjob.application.service.Redis.PermissionCacheService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class PermissionCacheInvalidationListener {
    private final PermissionCacheService cacheService;


    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handlePermissionChanged(PermissionCacheInvalidationEvent event) {
        if(!event.userIds().isEmpty()){
            for(Long userId: event.userIds()){
                cacheService.evict(userId);
            }
            cacheService.publishInvalidation(event.userIds());
        }
    }
}
