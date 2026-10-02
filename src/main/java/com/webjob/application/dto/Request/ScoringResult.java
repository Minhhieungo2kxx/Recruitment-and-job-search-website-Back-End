package com.webjob.application.dto.Request;

import com.webjob.application.enums.MatchType;
import com.webjob.application.models.Entity.CandidateSkill;
import com.webjob.application.models.Entity.JobSkill;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
public class ScoringResult {
    private final double matchScore;
    private final double skillScore;
    private final double experienceScore;
    private final double educationScore;
    private final double projectScore;
    private final double certificationScore;
    private final List<SkillMatchOutcome> skillOutcomes;



}
