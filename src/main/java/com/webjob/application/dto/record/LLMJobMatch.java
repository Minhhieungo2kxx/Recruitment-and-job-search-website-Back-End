package com.webjob.application.dto.record;

import java.util.List;

public record LLMJobMatch(
        Long jobId,
        Double matchScore,
        String reason,
        List<String> matchedSkills,
        List<String> missingSkills

) {
}
