package com.webjob.application.Model.Entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.webjob.application.Model.Enums.CompetitionLevel;
import com.webjob.application.Model.Enums.JobCategory;
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

    @NotNull(message = "Mức lương không được để trống")
    @PositiveOrZero(message = "Mức lương phải là số không âm")
    private double salary;

    @NotNull(message = "Số lượng không được để trống")
    @Min(value = 1, message = "Số lượng phải lớn hơn hoặc bằng 1")
    private int quantity;

    @NotBlank(message = "Cấp độ không được để trống")
    @Pattern(
            regexp = "^(INTERN|FRESHER|JUNIOR|MIDDLE|SENIOR)$",
            message = "Cấp độ không hợp lệ. Giá trị hợp lệ: INTERN, FRESHER, JUNIOR, MIDDLE, SENIOR"
    )
    private String level;

    @Column(name = "applied_count")
    @Min(value = 0, message = "Số lượng ứng viên đã ứng tuyển không được âm")
    private int appliedCount;

    @NotNull(message = "Trạng thái không được để trống")
    @Enumerated(EnumType.STRING) // Store enum as String in DB
    @Column(name = "competition_level", length = 10)
    private CompetitionLevel competitionLevel;

    @NotNull(message = "Loại công việc không được để trống")
    @Enumerated(EnumType.STRING)
    @Column(name = "job_category", length = 50)
    private JobCategory jobCategory;

    @Column(columnDefinition = "MEDIUMTEXT")
    @NotBlank(message = "Mô tả công việc không được để trống")
    private String description;

    @NotNull(message = "Thời hạn bắt đầu không được để trống")
    @JsonFormat(
            shape = JsonFormat.Shape.STRING,
            pattern = "yyyy-MM-dd HH:mm:ss a z",
            timezone = "Asia/Ho_Chi_Minh",
            locale = "en_US"
    )
    private Instant startDate;

    @NotNull(message = "Thời hạn kết thúc không được để trống")
    @JsonFormat(
            shape = JsonFormat.Shape.STRING,
            pattern = "yyyy-MM-dd HH:mm:ss a z",
            timezone = "Asia/Ho_Chi_Minh",
            locale = "en_US"
    )
    private Instant endDate;

    private boolean active;

    @Column(name = "created_at", updatable = false)
    @CreatedDate
    @JsonFormat(
            shape = JsonFormat.Shape.STRING,
            pattern = "yyyy-MM-dd HH:mm:ss a z",
            timezone = "Asia/Ho_Chi_Minh",
            locale = "en_US"
    )
    private Instant createdAt;

    @Column(name = "updated_at")
    @LastModifiedDate
    @JsonFormat(
            shape = JsonFormat.Shape.STRING,
            pattern = "yyyy-MM-dd HH:mm:ss a z",
            timezone = "Asia/Ho_Chi_Minh",
            locale = "en_US"
    )
    private Instant updatedAt;

    @Column(name = "created_by")
    @Size(max = 100, message = "Người tạo không được vượt quá 100 ký tự")
    @CreatedBy
    private String createdBy;

    @Column(name = "updated_by")
    @Size(max = 100, message = "Người cập nhật không được vượt quá 100 ký tự")
    @LastModifiedBy
    private String updatedBy;


    @ManyToOne
    @JoinColumn(name = "company_id")
    private Company company;

    @ManyToMany
    @JsonIgnoreProperties(value = { "jobs" })
    @JoinTable(
            name = "job_skill",
            joinColumns = @JoinColumn(name = "job_id"),
            inverseJoinColumns = @JoinColumn(name = "skill_id")
    )
    private List<Skill> skills;

    @OneToMany(mappedBy ="job",fetch =FetchType.LAZY)
    @JsonIgnore
    private List<Resume> resumeList;

    // Quan hệ 1-n với token reset
    @OneToMany(mappedBy = "job", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private List<Payment> payments;


}
