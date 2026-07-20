package com.webjob.application.models.Entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import com.webjob.application.enums.*;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
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
@Table(name = "jobs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class Job {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Tên công việc không được để trống")
    @Size(max = 255, message = "Tên công việc không được vượt quá 255 ký tự")
    private String name;

    @NotBlank(message = "Địa điểm không được để trống")
    @Size(max = 255, message = "Địa điểm không được vượt quá 255 ký tự")
    private String location;

    //    private double salary;
    @NotNull(message = "Mức lương không được để trống")
    @PositiveOrZero(message = "Mức lương phải là số không âm")
    private Double salaryMin;

    @NotNull(message = "Mức lương không được để trống")
    @PositiveOrZero(message = "Mức lương phải là số không âm")
    private Double salaryMax;

    private boolean negotiable;


    @NotNull(message = "Số lượng không được để trống")
    @Min(value = 1, message = "Số lượng phải lớn hơn hoặc bằng 1")
    private int quantity;


    @Enumerated(EnumType.STRING)
    private JobLevel level;

//    * Số năm kinh nghiệm tối thiểu yêu cầu
    private Integer experienceRequired =0;


    @Enumerated(EnumType.STRING)
    private WorkingType workingType;

    @Enumerated(EnumType.STRING)
    private WorkMode workMode;

    @Column(columnDefinition = "MEDIUMTEXT")
    private String benefits;

    @Column(columnDefinition = "MEDIUMTEXT")
    private String requirement;

    @Column(columnDefinition = "MEDIUMTEXT")
    private String responsibility;

    private Long viewCount= 0L;


    @Column(name = "applied_count")
    @Min(value = 0, message = "Số lượng ứng viên đã ứng tuyển không được âm")
    private int appliedCount= 0;

    @NotNull(message = "Trạng thái không được để trống")
    @Enumerated(EnumType.STRING) // Store enum as String in DB
    @Column(name = "competition_level", length = 10)
    private CompetitionLevel competitionLevel;


    @Column(columnDefinition = "MEDIUMTEXT")
    @NotBlank(message = "Mô tả công việc không được để trống")
    private String description;

    @NotNull(message = "Thời hạn bắt đầu không được để trống")
    private Instant startDate;

    @NotNull(message = "Thời hạn kết thúc không được để trống")
    private Instant endDate;

    //    private boolean active;
    @Enumerated(EnumType.STRING)
    private JobStatus status;

    @Column(name = "created_at", updatable = false)
    @CreatedDate
    private Instant createdAt;

    @Column(name = "updated_at")
    @LastModifiedDate
    private Instant updatedAt;

    @Column(name = "created_by")
    @Size(max = 100, message = "Người tạo không được vượt quá 100 ký tự")
    @CreatedBy
    private String createdBy;

    @Column(name = "updated_by")
    @Size(max = 100, message = "Người cập nhật không được vượt quá 100 ký tự")
    @LastModifiedBy
    private String updatedBy;

    @Column(nullable = false)
    private boolean deleted = false;

    private Instant deletedAt;

    private String deletedBy;


    @ManyToOne
    @JoinColumn(name = "company_id")
    private Company company;


    @OneToMany(
            mappedBy = "job",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    @JsonIgnore
    private List<JobSkill> jobSkills = new ArrayList<>();


    // Quan hệ 1-n với token reset
    @OneToMany(mappedBy = "job", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private List<Payment> payments  = new ArrayList<>();

    @OneToMany(mappedBy = "job", fetch = FetchType.LAZY)
    @JsonIgnore
    private List<SavedJob> savedByUsers  = new ArrayList<>();

    @ManyToOne
    @JoinColumn(name = "job_category")
    private JobCategory jobCategory;

    @OneToMany(mappedBy = "job")
    @JsonIgnore
    private List<Application> applications = new ArrayList<>();


}
