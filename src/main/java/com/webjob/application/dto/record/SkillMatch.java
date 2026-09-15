package com.webjob.application.dto.record;

import com.webjob.application.models.Entity.Skill;

public record SkillMatch(
        String normalizedSkillName,
        Skill skill,
        int start,
        int end
) {

    public int length() {
        return end - start + 1;
    }
}
