package com.webjob.application.mapper;

import com.webjob.application.dto.Response.SkillResponse;
import com.webjob.application.models.Entity.Skill;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class SkillMapper {
    public SkillResponse toResponse(Skill skill) {
        if (skill == null) {
            return null;
        }

        return SkillResponse.builder()
                .id(skill.getId())
                .name(skill.getName())
                .description(skill.getDescription())
                .status(skill.getStatus())
                .createdAt(skill.getCreatedAt())
                .createdBy(skill.getCreatedBy())
                .build();
    }


}
