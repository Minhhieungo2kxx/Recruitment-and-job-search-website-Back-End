package com.webjob.application.dto.record;

import java.util.List;

public record PermissionCacheInvalidationEvent(
        List<Long> userIds
) {
}
