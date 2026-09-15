package com.webjob.application.models.Entity;

import com.webjob.application.enums.SkillAliasType;
import com.webjob.application.enums.SkillStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@EntityListeners(AuditingEntityListener.class)
@Table(
        name = "skill_alias",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_skill_alias_normalized",
                        columnNames = {"skill_id", "normalized_alias"}
                )
        },
        indexes = {
                @Index(
                        name = "idx_skill_alias_normalized",
                        columnList = "normalized_alias"
                )
        }
)
public class SkillAlias {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "skill_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_skill_alias_skill")
    )
    private Skill skill;

    /**
     * Alias có thể xuất hiện trong CV.
     *
     * Ví dụ:
     * K8s
     * ReactJS
     * React.js
     * PostgreSQL
     * Postgres
     * PSQL
     */
    @NotBlank(message = "Alias không được để trống")
    @Size(max = 100, message = "Alias không được vượt quá 100 ký tự")
    @Column(nullable = false, length = 100)
    private String alias;

    /**
     * Giá trị normalize để phục vụ matching CV.
     *
     * Ví dụ:
     *
     * ReactJS  -> reactjs
     * React.js -> react.js
     * Postgres -> postgres
     */
    @NotBlank(message = "Normalized alias không được để trống")
    @Size(max = 100, message = "Normalized alias không được vượt quá 100 ký tự")
    @Column(
            name = "normalized_alias",
            nullable = false,
            length = 100
    )
    private String normalizedAlias;

    /**
     * ALIAS
     * ABBREVIATION
     * VARIANT
     */
    @NotNull(message = "Loại alias không được để trống")
    @Enumerated(EnumType.STRING)
    @Column(name = "alias_type", nullable = false, length = 20)
    private SkillAliasType aliasType = SkillAliasType.ALIAS;

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
}
