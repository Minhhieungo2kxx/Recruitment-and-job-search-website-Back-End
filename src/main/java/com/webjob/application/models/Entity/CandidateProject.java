package com.webjob.application.models.Entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
        name = "candidate_projects",
        indexes = {
                @Index(
                        name = "idx_candidate_project_profile",
                        columnList = "candidate_profile_id"
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CandidateProject {
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
    private String name;

    @Lob
    private String description;

    @Column(name = "technologies", columnDefinition = "TEXT")
    private String technologies;


    @Column(columnDefinition = "TEXT")
    private String role;


    private String relevance; // RELEVANT / PARTIAL / NOT_RELEVANT so với yêu cầu Job
}
