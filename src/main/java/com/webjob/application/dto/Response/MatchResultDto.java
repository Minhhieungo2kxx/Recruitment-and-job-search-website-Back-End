package com.webjob.application.dto.Response;

import com.webjob.application.enums.MatchStatus;
import com.webjob.application.enums.MatchType;
import com.webjob.application.enums.ResumeStatus;
import lombok.Builder;

import java.time.Instant;
import java.util.List;

public class MatchResultDto {
    @Builder
    public record Summary(
            Long applicationId,
            Long candidateUserId,
            String candidateName,
            Double matchScore,
            MatchStatus matchStatus,
            ResumeStatus resumeStatus,
            Instant matchedAt
    ) {
    }

    @Builder
    public record Detail(
            Long applicationId,
            Double matchScore,
            Double skillScore,
            Double experienceScore,
            Double educationScore,
            Double projectScore,
            Double certificationScore,
            List<String> strengths,
            List<String> concerns,
            String experienceAssessment,
            String educationAssessment,
            String overallReason,

            List<String> interviewFocus,
            List<String> verificationItems,
            List<String> considerations,

            List<SkillDetail> skillDetails,
            MatchStatus status,
            String errorMessage,
            Instant matchedAt
    ) {
    }

    @Builder
    public record SkillDetail(
            String skillName,
            boolean required,
            MatchType matchType,
            String note
    ) {
    }
}
