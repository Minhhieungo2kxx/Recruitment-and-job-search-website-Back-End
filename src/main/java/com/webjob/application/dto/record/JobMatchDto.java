package com.webjob.application.dto.record;

import com.webjob.application.enums.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record JobMatchDto(
        Long jobId,
        String jobName,

        // Company
        Long companyId,
        String companyName,
        String companyLogo,

        // Match
        BigDecimal matchScore,
        String matchReason,
        List<String> matchedSkills,
        List<String> missingSkills,

        // Job information
        String location,
        Double salaryMin,
        Double salaryMax,
        boolean negotiable,
        JobLevel level,
        Integer experienceRequired,
        WorkingType workingType,
        WorkMode workMode,

        // Category
        Long categoryId,
        String categoryName,

        // Recruitment
        int quantity,
        int appliedCount,
        CompetitionLevel competitionLevel,

        // Time
        Instant startDate,
        Instant endDate,

        // Statistics
        Long viewCount,

        // Status
        JobStatus status,

        // Metadata
        Instant createdAt

) {
}
