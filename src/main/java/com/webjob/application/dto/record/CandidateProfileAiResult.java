package com.webjob.application.dto.record;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.webjob.application.dto.Request.CandidateProfileDetails;
import com.webjob.application.dto.Request.ParsedSkill;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
public record CandidateProfileAiResult(
        String summary,
        Double totalExperienceYears,
//        List<String> rawSkillTexts, // đưa qua SkillNormalizationService.normalize(...)
        List<ParsedSkill> parsedSkills,
        CandidateProfileDetails details,
        String aiModel,
        String aiPromptVersion
) {
}
