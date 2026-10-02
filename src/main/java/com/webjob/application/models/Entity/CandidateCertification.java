package com.webjob.application.models.Entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(
        name = "candidate_certifications",
        indexes = {
                @Index(
                        name = "idx_candidate_certification_profile",
                        columnList = "candidate_profile_id"
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CandidateCertification {
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

    @Column(length = 255)
    private String issuer;

    private LocalDate issueDate;

    private LocalDate expireDate;
}
