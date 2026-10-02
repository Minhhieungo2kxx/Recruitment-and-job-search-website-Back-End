package com.webjob.application.dto.record;

import com.webjob.application.enums.SkillSource;
import com.webjob.application.models.Entity.Skill;

public record SkillMatchingCV(
        Skill skill,
        SkillSource source,
        double confidence,
        String matchedKeyword
) {
}
