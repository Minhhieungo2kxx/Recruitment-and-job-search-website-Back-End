package com.webjob.application.dto.Request;

import com.webjob.application.enums.MatchType;
import com.webjob.application.models.Entity.CandidateSkill;
import com.webjob.application.models.Entity.JobSkill;
import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@Builder
public class SkillMatchOutcome {
    private final JobSkill jobSkill;
    private final CandidateSkill candidateSkill; // null nếu MISSING
    private final MatchType matchType;
    private final String note;
    /**
     * Mức độ match skill: 0.0 -> 1.0
     */
    private double matchScore;

    /**
     * Mức độ đáp ứng kinh nghiệm: 0.0 -> 1.0
     */
    private double experienceScore;

    /**
     * Mức độ đáp ứng level: 0.0 -> 1.0
     */
    private double levelScore;

    /**
     * Độ tin cậy của việc mapping skill.
     * Không phải AI thì mặc định 1.0
     */
    private double confidenceScore;

    /**
     * Trọng số lấy từ JobSkill.priority
     */
    private double weight;

    /**
     * Điểm cuối cùng của skill trước khi áp dụng priority.
     * 0.0 -> 1.0
     */
    private double finalScore;
}
