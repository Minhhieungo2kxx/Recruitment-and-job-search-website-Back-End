package com.webjob.application.mapper;

import com.webjob.application.dto.Response.JobCategoryResponse;
import com.webjob.application.dto.Response.JobCategoryResponseChildren;
import com.webjob.application.dto.Response.JobCategoryTreeResponse;
import com.webjob.application.models.Entity.JobCategory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;

@Component
public class JobCategoryMapper {
    public JobCategoryResponse toResponse(JobCategory category) {
        if (category == null) {
            return null;
        }

        return JobCategoryResponse.builder()
                .id(category.getId())
                .name(category.getName())
                .description(category.getDescription())
                .level(category.getLevel())
                .status(category.getStatus())
                .parentId(category.getParent() != null ? category.getParent().getId() : null)

                .skills(category.getJobCategorySkills() == null
                        ? Collections.emptyList()
                        : category.getJobCategorySkills().stream()
                        .map(s -> JobCategoryResponse.JobCategorySkillResponse.builder()
                                .id(s.getId())
                                .skillId(s.getSkill() != null ? s.getSkill().getId() : null)
                                .skillName(s.getSkill() != null ? s.getSkill().getName() : null)
                                .level(s.getLevel())
                                .required(s.getRequired())
                                .weight(s.getWeight())
                                .build()
                        ).toList()
                )
                .build();
    }

    public JobCategoryResponseChildren toResponseTreeChildren(JobCategory entity) {
        if (entity == null) {
            return null;
        }

        return JobCategoryResponseChildren.builder()
                .id(entity.getId())
                .name(entity.getName())
                .description(entity.getDescription())
                .level(entity.getLevel())
                .status(entity.getStatus())
                .parentId(entity.getParent() != null ? entity.getParent().getId() : null)
                .childrenCount(entity.getChildren().size())
                .children(entity.getChildren() == null ? Collections.emptyList()
                        : entity.getChildren()
                        .stream()
                        .map(this::toResponseTreeChildren)
                        .toList()).build();

    }

    public JobCategoryTreeResponse toResponseTree(JobCategory category, Map<Long, Long> jobCountMap) {
        if (category == null) {
            return null;
        }
        return JobCategoryTreeResponse.builder()
                .id(category.getId())
                .name(category.getName())
                .level(category.getLevel())
                .totalJobs(jobCountMap.getOrDefault(category.getId(), 0L))
                .children(new ArrayList<>())
                .build();

    }

}
