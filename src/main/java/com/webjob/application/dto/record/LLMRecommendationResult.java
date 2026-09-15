package com.webjob.application.dto.record;

import java.util.List;

public record LLMRecommendationResult(
        List<LLMJobMatch> recommendations
) {
}
