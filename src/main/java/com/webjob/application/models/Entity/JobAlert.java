package com.webjob.application.models.Entity;

import com.webjob.application.enums.AlertFrequency;

import com.webjob.application.enums.JobLevel;
import com.webjob.application.enums.WorkMode;
import com.webjob.application.enums.WorkingType;
import jakarta.persistence.*;
import lombok.Data;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;

@Entity
@Table(name = "job_alerts",
        indexes = {
                @Index(name="idx_next_run", columnList="next_run_at"),
                @Index(name="idx_active_next_run", columnList="active,next_run_at")
        }
)
@EntityListeners(AuditingEntityListener.class)
@Data
public class JobAlert {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(nullable = false)
    private User user;

    // Java Backend
    private String keyword;

    // Hà Nội
    private String location;

    // 20 triệu
    private Double salaryMin;

    // 40 triệu
    private Double salaryMax;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_category_id")
    private JobCategory jobCategory;

    @Enumerated(EnumType.STRING)
    private JobLevel level;

    @Enumerated(EnumType.STRING)
    private AlertFrequency frequency;

    @Enumerated(EnumType.STRING)
    private WorkMode workMode;

    @Enumerated(EnumType.STRING)
    private WorkingType workingType;

    private Boolean active = true;

    private Instant lastCheckedAt; // Ghi nhận lần xử lý gần nhất

    private Instant nextRunAt; // Scheduler dùng để query

    @CreatedDate
    private Instant createdAt;
}
