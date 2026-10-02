package com.webjob.application.models.Entity;

import com.webjob.application.enums.ParseStatus;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Hồ sơ ứng viên đã được AI parse + chuẩn hoá từ 1 UserResume.
 * Quan hệ 1-1 với UserResume: 1 CV được parse 1 lần, tái sử dụng cho mọi lần
 * matching sau đó (kể cả khi ứng tuyển nhiều Job khác nhau bằng cùng 1 CV).
 */

@Entity
@Table(
        name = "candidate_profiles",
        uniqueConstraints = @UniqueConstraint(columnNames = "resume_id")
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
public class CandidateProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "resume_id", nullable = false, unique = true)
    private UserResume resume;

    // Text thô trích xuất từ file (Tika)
    @Lob
    @Column(name = "raw_text", columnDefinition = "LONGTEXT")
    private String rawText;

    @Column(columnDefinition = "TEXT")
    private String summary;

    @Column(name = "total_experience_years")
    private Double totalExperienceYears;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "parse_status",
            nullable = false,
            length = 20
    )
    @Builder.Default
    private ParseStatus parseStatus = ParseStatus.PENDING;

    @Column(name = "parse_error", columnDefinition = "TEXT")
    private String parseError;

    @Column(name = "ai_model", length = 50)
    private String aiModel;

    @Column(name = "ai_prompt_version", length = 20)
    private String aiPromptVersion;

    @Column(name = "parsed_at")
    private Instant parsedAt;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private Instant updatedAt;

    @OneToMany(
            mappedBy = "candidateProfile",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.LAZY
    )
    @OrderBy("startDate DESC")
    @Builder.Default
    private List<CandidateExperience> experiences = new ArrayList<>();

    @OneToMany(
            mappedBy = "candidateProfile",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.LAZY
    )
    @OrderBy("endDate DESC")
    @Builder.Default
    private List<CandidateEducation> educations = new ArrayList<>();

    @OneToMany(
            mappedBy = "candidateProfile",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.LAZY
    )
    @Builder.Default
    private List<CandidateProject> projects = new ArrayList<>();

    @OneToMany(
            mappedBy = "candidateProfile",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.LAZY
    )
    @OrderBy("issueDate DESC")
    @Builder.Default
    private List<CandidateCertification> certifications = new ArrayList<>();

    @OneToMany(
            mappedBy = "candidateProfile",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.LAZY
    )
    @Builder.Default
    private List<CandidateSkill> skills = new ArrayList<>();



}
