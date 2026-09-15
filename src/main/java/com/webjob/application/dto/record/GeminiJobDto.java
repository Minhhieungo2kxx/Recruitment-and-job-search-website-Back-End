package com.webjob.application.dto.record;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
public record GeminiJobDto(
        Long jobId,
        String name,

        String companyName,
        String location,
        String level,
        int experienceRequired,
        List<String> requiredSkills,
        String workMode,
        Double salaryMin,
        Double salaryMax,
        String jobCategory,
        String workingType,
        String requirement

) {
}
