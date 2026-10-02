package com.webjob.application.dto.record;

import java.util.List;

public record ExplanationResult(
        List<String> strengths,
        List<String> concerns,
        String experienceAssessment,
        String educationAssessment,
        String overallReason,
        DecisionSupport decisionSupport,
        String aiModel,
        String aiPromptVersion
) {
}
