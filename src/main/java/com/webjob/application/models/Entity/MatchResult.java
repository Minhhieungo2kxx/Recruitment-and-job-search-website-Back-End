package com.webjob.application.models.Entity;


import com.webjob.application.enums.MatchStatus;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
        name = "match_results",
        uniqueConstraints = @UniqueConstraint(columnNames = "application_id"),
        indexes = @Index(name = "idx_match_result_score", columnList = "match_score")
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
public class MatchResult {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "application_id", nullable = false, unique = true)
    private Application application;

    @Column(name = "match_score")
    private Double matchScore;

    // ==== Breakdown điểm theo trọng số - xem MatchingScoringEngine ====
    @Column(name = "skill_score")
    private Double skillScore;

    @Column(name = "experience_score")
    private Double experienceScore;

    @Column(name = "education_score")
    private Double educationScore;

    @Column(name = "project_score")
    private Double projectScore;

    @Column(name = "certification_score")
    private Double certificationScore;

    // ==== Explanation do AI sinh, dựa trên score đã tính (không tự đổi điểm) ====
    @Column(name = "strengths", columnDefinition = "JSON")
    private String strengthsJson;      // List<String> serialize sẵn - xem MatchResultDto

    @Column(name = "concerns", columnDefinition = "JSON")
    private String concernsJson;       // List<String>

    @Column(name = "interviewFocus", columnDefinition = "JSON")
    private String interviewFocus;       // List<String>

    @Column(name = "verificationItems", columnDefinition = "JSON")
    private String verificationItems;       // List<String>

    @Column(name = "considerations", columnDefinition = "JSON")
    private String considerations;       // List<String>



    @Column(name = "experience_assessment", columnDefinition = "TEXT")
    private String experienceAssessment;

    @Column(name = "education_assessment", columnDefinition = "TEXT")
    private String educationAssessment;

    @Column(name = "overall_reason", columnDefinition = "TEXT")
    private String overallReason;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private MatchStatus status = MatchStatus.PENDING;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "ai_model", length = 50)
    private String aiModel;

    @Column(name = "ai_prompt_version", length = 20)
    private String aiPromptVersion;

    @Column(name = "matched_at")
    private Instant matchedAt;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private Instant updatedAt;

    @OneToMany(mappedBy = "matchResult", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<MatchResultSkillDetail> skillDetails = new ArrayList<>();

    public void clearSkillDetails() {
        skillDetails.clear();
    }

    public void addSkillDetail(MatchResultSkillDetail detail) {
        skillDetails.add(detail);
        detail.setMatchResult(this);
    }

}
