package com.webjob.application.models.Entity;

import com.webjob.application.enums.SkillLevel;
import com.webjob.application.enums.SkillSource;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;

@Entity
@Table(
        name = "candidate_skills",
        indexes = @Index(name = "idx_candidate_skill_profile", columnList = "candidate_profile_id, skill_id")
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
public class CandidateSkill {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "candidate_profile_id", nullable = false)
    private CandidateProfile candidateProfile;

    /** null nếu source = UNMATCHED (skill lạ, không map được vào danh mục Skill chuẩn) */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "skill_id")
    private Skill skill;

    /** Text gốc trong CV, vd "ReactJS", "Node" - giữ lại để audit/debug việc chuẩn hoá */
    @Column(name = "raw_text", length = 150, nullable = false)
    private String rawText;

    @Column(name = "years_of_experience")
    private Double yearsOfExperience;

    @Enumerated(EnumType.STRING)
    private SkillLevel level;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SkillSource source;

    /** 0-1, độ tin cậy khi source = MATCHED_BY_AI; null khi MATCHED_BY_ALIAS (coi như 1.0) */
    @Column
    private Double confidence;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

}
