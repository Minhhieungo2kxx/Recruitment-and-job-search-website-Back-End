package com.webjob.application.models.Entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(
        name = "candidate_educations",
        indexes = {
                @Index(
                        name = "idx_candidate_education_profile",
                        columnList = "candidate_profile_id"
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CandidateEducation {
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
    private String school;

    @Column(length = 100)
    private String degree;

    @Column(length = 255)
    private String major;

    private LocalDate startDate;

    private LocalDate endDate;

    @Column
    private Double gpa;

    private String relevance; // RELEVANT / PARTIAL / NOT_RELEVANT so với yêu cầu Job




}
