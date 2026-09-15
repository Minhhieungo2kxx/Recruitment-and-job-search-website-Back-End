package com.webjob.application.dto.record;

import com.webjob.application.models.Entity.Skill;

public record SkillEntry(
        String normalizedName,
        Skill skill
) {
}
