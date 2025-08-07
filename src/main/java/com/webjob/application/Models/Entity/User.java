package com.webjob.application.Models.Entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.webjob.application.Models.Entity.Company;
import com.webjob.application.Models.Entity.Resume;
import com.webjob.application.Models.Entity.Role;
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
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @NotBlank(message = "Email không được để trống")
    @Email(message = "Email không đúng định dạng")
    @Size(max = 255, message = "Email không được vượt quá 255 ký tự")
    private String email;

    @NotBlank(message = "Mật khẩu không được để trống")
    @Size(min = 8, message = "Mật khẩu phải có ít nhất 8 ký tự")
    private String password;

    @NotBlank(message = "Họ tên không được để trống")
    @Size(max = 100, message = "Họ tên không được vượt quá 100 ký tự")
    private String fullName;

    @Size(max = 500, message = "Đường dẫn avatar không được vượt quá 500 ký tự")
    private String avatar;

    @NotNull(message = "Tuổi không được để trống")
    @Min(value = 0, message = "Tuổi không được nhỏ hơn 0")
    @Max(value = 150, message = "Tuổi không được lớn hơn 150")
    private Integer age;

    @NotBlank(message = "Giới tính không được để trống")
    @Pattern(regexp = "MALE|FEMALE", message = "Giới tính phải là MALE hoặc FEMALE")
    private String gender; // MALE/FEMALE

    @NotBlank(message = "Địa chỉ không được để trống")
    @Size(max = 500, message = "Địa chỉ không được vượt quá 500 ký tự")
    private String address;

    @Column(length = 512)
    private String refreshToken;


    @CreatedDate
    @Column(name = "created_at", updatable = false)
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

    @ManyToOne
    @JoinColumn(name ="company_id")
    private Company company;

    @OneToMany(mappedBy ="user",fetch =FetchType.LAZY)
    @JsonIgnore
    private List<Resume> resumeList;

    @ManyToOne
    @JoinColumn(name = "role_id")
    private Role role;

    // Quan hệ 1-n với token reset
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private List<PasswordResetToken> passwordResetTokens;



}
