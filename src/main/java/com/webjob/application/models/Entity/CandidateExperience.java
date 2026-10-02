package com.webjob.application.models.Entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(
        name = "candidate_experiences",
        indexes = {
                @Index(
                        name = "idx_candidate_experience_profile",
                        columnList = "candidate_profile_id"
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CandidateExperience {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "candidate_profile_id",
            nullable = false
    )
    private CandidateProfile candidateProfile;

    @Column(length = 255)
    private String company;

    @Column(length = 255)
    private String title;

    private LocalDate startDate;

    private LocalDate endDate;

    @Column(name = "is_current", nullable = false)
    @Builder.Default
    private boolean current = false;

    @Lob
    private String description;

    /**
     * Duration calculated by parser.
     * Unit: month.
     */
    @Column(name = "duration_months")
    private Integer durationMonths;
}
