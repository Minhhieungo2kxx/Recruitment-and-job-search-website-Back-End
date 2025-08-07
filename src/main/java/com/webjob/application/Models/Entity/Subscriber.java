package com.webjob.application.Models.Entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.webjob.application.Models.Entity.Skill;
import com.webjob.application.Models.Enums.SubscriberStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.List;

@Entity
@Table(name = "subscribers")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class Subscriber {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Tên không được để trống")
    private String name;

    @NotBlank(message = "Email không được để trống")
    @Email(message = "Email không hợp lệ")
    private String email;

    @Pattern(
            regexp = "^\\+?[0-9]{9,15}$",
            message = "Số điện thoại không hợp lệ. Chỉ bao gồm số và có thể bắt đầu bằng '+'"
    )
    private String phoneNumber;

    @Size(max = 500, message = "Mô tả không được vượt quá 500 ký tự")
    private String description;

    @Enumerated(EnumType.STRING)
    private SubscriberStatus status = SubscriberStatus.ACTIVE;

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

    @ManyToMany(fetch = FetchType.LAZY)
    @JsonIgnoreProperties(value = { "subscribers" }) // để tránh vòng lặp JSON
    @JoinTable(
            name = "subscriber_skill",
            joinColumns = @JoinColumn(name = "subscriber_id"),
            inverseJoinColumns = @JoinColumn(name = "skill_id")
    )
    private List<Skill> skills;
}
