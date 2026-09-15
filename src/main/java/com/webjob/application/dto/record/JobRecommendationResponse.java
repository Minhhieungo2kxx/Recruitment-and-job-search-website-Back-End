package com.webjob.application.dto.record;

import java.time.Instant;
import java.util.List;

public record JobRecommendationResponse(
        Long recommendationId,
        Instant generatedAt,
        List<JobMatchDto> matches
) {
}
