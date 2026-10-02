package com.webjob.application.models.Entity;



import com.webjob.application.enums.MatchType;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
        name = "match_result_skill_details",
        indexes = @Index(name = "idx_mrsd_result", columnList = "match_result_id")
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MatchResultSkillDetail {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "match_result_id", nullable = false)
    private MatchResult matchResult;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_skill_id", nullable = false)
    private JobSkill jobSkill;

    /** null nếu matchType = MISSING */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "candidate_skill_id")
    private CandidateSkill candidateSkill;

    @Enumerated(EnumType.STRING)
    @Column(name = "match_type", nullable = false, length = 20)
    private MatchType matchType;

    /** vd "Có React,... nhưng thiếu 1 năm kinh nghiệm yêu cầu (2/3 năm)" */
    @Column(columnDefinition = "TEXT")
    private String note;
}
