package com.webjob.application.dto.record;

public record RecommendationCacheInvalidateEvent(
        Long userId,
        String reason
) {
}
