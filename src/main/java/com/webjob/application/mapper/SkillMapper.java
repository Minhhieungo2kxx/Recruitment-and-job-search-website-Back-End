package com.webjob.application.mapper;

import com.webjob.application.dto.Response.SkillResponse;
import com.webjob.application.models.Entity.Skill;
import com.webjob.application.models.Entity.SkillAlias;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

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
                .skillAliasResponses(
                        skill.getAliases()
                                .stream()
                                .map(alias -> SkillResponse.SkillAliasResponse.builder()
                                        .alias(alias.getAlias())
                                        .normalizedAlias(alias.getNormalizedAlias())
                                        .aliasType(alias.getAliasType())
                                        .status(alias.getStatus())
                                        .build()).collect(Collectors.toList()))
                .build();
    }

    public SkillResponse toResponsePage(
            Skill skill,
            List<SkillAlias> aliases
    ) {
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
                .skillAliasResponses(
                        aliases.stream()
                                .map(alias -> SkillResponse.SkillAliasResponse.builder()
                                        .alias(alias.getAlias())
                                        .normalizedAlias(alias.getNormalizedAlias())
                                        .aliasType(alias.getAliasType())
                                        .status(alias.getStatus())
                                        .build())
                                .collect(Collectors.toList())
                )
                .build();
    }



}
