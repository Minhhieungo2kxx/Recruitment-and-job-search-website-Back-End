package com.webjob.application.dto.record;

import com.webjob.application.models.Entity.Job;

public record SkillMatchResult(
        Job job,
        Long matchedSkillCount
) {
}
