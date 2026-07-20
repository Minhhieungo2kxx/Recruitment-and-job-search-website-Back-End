package com.webjob.application.models.Entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.webjob.application.enums.CategoryStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "job_categories")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class JobCategory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Size(max = 100)
    @Column(nullable = false, unique = true, length = 100)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    @Min(value = 0, message = "Level phải lớn hơn hoặc bằng 0")
    @Max(value = 5, message = "Level phải nhỏ hơn hoặc bằng 5")
    private Integer level = 0;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CategoryStatus status = CategoryStatus.ACTIVE;

    @CreatedDate
    private Instant createdAt;

    @LastModifiedDate
    private Instant updatedAt;

    @CreatedBy
    private String createdBy;

    @LastModifiedBy
    private String updatedBy;

    @OneToMany(mappedBy = "jobCategory", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private List<JobCategorySkill> jobCategorySkills = new ArrayList<>();

    @OneToMany(mappedBy = "jobCategory", fetch = FetchType.LAZY)
    @JsonIgnore
    private List<Job> jobs;

    @OneToMany(mappedBy = "jobCategory", fetch = FetchType.LAZY)
    @JsonIgnore
    private List<JobAlert> jobAlerts;

    // parent category
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private JobCategory parent;

    // child categories
    @OneToMany(mappedBy = "parent")
    @JsonIgnore
    private List<JobCategory> children = new ArrayList<>();
}
