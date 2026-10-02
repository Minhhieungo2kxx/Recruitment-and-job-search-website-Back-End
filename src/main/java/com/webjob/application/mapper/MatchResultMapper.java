package com.webjob.application.mapper;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.webjob.application.dto.Response.MatchResultDto;
import com.webjob.application.models.Entity.MatchResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class MatchResultMapper {
    private final ObjectMapper objectMapper;

    public MatchResultDto.Summary toSummary(MatchResult mr) {
        return MatchResultDto.Summary.builder()
                .applicationId(mr.getApplication().getId())
                .candidateUserId(mr.getApplication().getUser().getId())
                .candidateName(mr.getApplication().getUser().getFullName())
                .matchScore(mr.getMatchScore())
                .matchStatus(mr.getStatus())
                .resumeStatus(mr.getApplication().getStatus())
                .matchedAt(mr.getMatchedAt())
                .build();
    }
    public MatchResultDto.Detail toDetail(MatchResult mr) {
        return MatchResultDto.Detail.builder()
                .applicationId(mr.getApplication().getId())
                .matchScore(mr.getMatchScore())
                .skillScore(mr.getSkillScore())
                .experienceScore(mr.getExperienceScore())
                .educationScore(mr.getEducationScore())
                .projectScore(mr.getProjectScore())
                .certificationScore(mr.getCertificationScore())
                .strengths(toList(mr.getStrengthsJson()))
                .concerns(toList(mr.getConcernsJson()))
                .experienceAssessment(mr.getExperienceAssessment())
                .educationAssessment(mr.getEducationAssessment())
                .overallReason(mr.getOverallReason())
                .interviewFocus(toList(mr.getInterviewFocus()))
                .verificationItems(toList(mr.getVerificationItems()))
                .considerations(toList(mr.getConsiderations()))
                .status(mr.getStatus())
                .errorMessage(mr.getErrorMessage())
                .matchedAt(mr.getMatchedAt())
                .skillDetails(mr.getSkillDetails().stream()
                        .map(d -> MatchResultDto.SkillDetail.builder()
                                .skillName(d.getJobSkill().getSkill().getName())
                                .required(d.getJobSkill().getRequired())
                                .matchType(d.getMatchType())
                                .note(d.getNote())
                                .build())
                        .toList())
                .build();
    }

    private List<String> toList(String json) {
        if (json == null || json.isBlank()) return List.of();
        try {
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (Exception e) {
            return List.of();
        }
    }


}
