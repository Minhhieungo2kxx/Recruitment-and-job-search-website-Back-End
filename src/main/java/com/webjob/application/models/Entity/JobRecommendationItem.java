package com.webjob.application.models.Entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(
        name = "job_recommendation_items",
        uniqueConstraints = @UniqueConstraint(columnNames = {"recommendation_id", "job_id"}),
        indexes = @Index(name = "idx_jri_score", columnList = "recommendation_id, match_score DESC")
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JobRecommendationItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recommendation_id", nullable = false)
    @JsonIgnore
    private JobRecommendation recommendation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_id", nullable = false)
    private Job job;

    @Column(name = "match_score", precision = 5, scale = 2, nullable = false)
    private BigDecimal matchScore;

    @Column(name = "match_reason", columnDefinition = "MEDIUMTEXT")
    private String matchReason;

    @Column(name = "matched_skills", columnDefinition = "TEXT")
    private String matchedSkills; // JSON string

    @Column(name = "missing_skills", columnDefinition = "TEXT")
    private String missingSkills; // JSON string

    @Column(name = "item_rank", nullable = false)
    private Integer rank;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;
}
