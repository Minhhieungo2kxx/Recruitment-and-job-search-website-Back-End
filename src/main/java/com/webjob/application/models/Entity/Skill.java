package com.webjob.application.models.Entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.webjob.application.enums.SkillStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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
import java.util.Objects;

@Entity
@Table(
        name = "skills",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = "name")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class Skill {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Tên skill chuẩn trong hệ thống.
     *
     * Ví dụ:
     * Java
     * Spring Boot
     * React
     * PostgreSQL
     * SEO
     */
    @NotBlank(message = "Tên kỹ năng không được để trống")
    @Size(max = 100, message = "Tên kỹ năng không được vượt quá 100 ký tự")
    @Column(nullable = false, length = 100)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    /**
     * ACTIVE / INACTIVE
     */
    @NotNull(message = "Trạng thái không được để trống")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SkillStatus status = SkillStatus.ACTIVE;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private Instant updatedAt;

    @CreatedBy
    @Size(max = 100, message = "Người tạo không được vượt quá 100 ký tự")
    @Column(name = "created_by", updatable = false, length = 100)
    private String createdBy;

    @LastModifiedBy
    @Size(max = 100, message = "Người cập nhật không được vượt quá 100 ký tự")
    @Column(name = "updated_by", length = 100)
    private String updatedBy;

    @OneToMany(mappedBy = "skill", fetch = FetchType.LAZY)
    @JsonIgnore
    private List<JobSkill> jobSkills = new ArrayList<>();

    @OneToMany(mappedBy = "skill", fetch = FetchType.LAZY)
    @JsonIgnore
    private List<SubscriberSkill> subscriberSkills = new ArrayList<>();

    @OneToMany(mappedBy = "skill", fetch = FetchType.LAZY)
    @JsonIgnore
    private List<JobCategorySkill> jobCategorySkills = new ArrayList<>();

    /**
     * Các alias dùng để nhận diện skill khi phân tích/matching CV.
     *
     * Ví dụ:
     *
     * Skill: Kubernetes
     * Alias:
     * - Kubernetes
     * - K8s
     * - Kube
     */
    @OneToMany(
            mappedBy = "skill",
            fetch = FetchType.LAZY,
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    @JsonIgnore
    private List<SkillAlias> aliases = new ArrayList<>();


    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (!(o instanceof Skill skill)) {
            return false;
        }

        return Objects.equals(id, skill.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }






}
