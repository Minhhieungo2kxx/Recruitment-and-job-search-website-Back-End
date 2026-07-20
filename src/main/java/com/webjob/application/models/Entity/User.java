package com.webjob.application.models.Entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.webjob.application.enums.UserStatus;
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
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

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
    private Long id;

    @NotBlank(message = "Email không được để trống")
    @Email(message = "Email không đúng định dạng")
    @Size(max = 255, message = "Email không được vượt quá 255 ký tự")
    @Column(unique = true, nullable = false)
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
    private LocalDate dateOfBirth;

    @NotBlank(message = "Giới tính không được để trống")
    @Pattern(regexp = "MALE|FEMALE", message = "Giới tính phải là MALE hoặc FEMALE")
    private String gender; // MALE/FEMALE

    private String phone;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private UserStatus status = UserStatus.ACTIVE; //

    @Column(name = "deleted")
    private boolean deleted = false;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    @NotBlank(message = "Địa chỉ không được để trống")
    @Size(max = 500, message = "Địa chỉ không được vượt quá 500 ký tự")
    private String address;


    @Column(columnDefinition = "MEDIUMTEXT")
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

    // THÊM TRƯỜNG MỚI ĐỂ LƯU THỜI GIAN TRUY CẬP CUỐI
    @Column(name = "last_seen_at")
    private Instant lastSeenAt;

    // THÊM TRƯỜNG TRẠNG THÁI ONLINE
    @Column(name = "is_online")
    private boolean isOnline = false;


    @ManyToOne
    @JoinColumn(name = "company_id")
    private Company company;



    @ManyToOne
    @JoinColumn(name = "role_id")
    private Role role;


    // Quan hệ 1-n với token reset
    @OneToMany(mappedBy = "user")
    @JsonIgnore
    private List<Payment> payments= new ArrayList<>();

    @OneToMany(mappedBy = "user")
    @JsonIgnore
    private List<ChatMessage> chatMessages= new ArrayList<>();

    @OneToMany(mappedBy = "user", fetch = FetchType.LAZY,
            cascade = CascadeType.ALL,
            orphanRemoval = true)
    @JsonIgnore
    private List<SavedJob> savedJobs= new ArrayList<>();

    @OneToMany(mappedBy = "user", fetch = FetchType.LAZY,
            cascade = CascadeType.ALL,
            orphanRemoval = true)
    @JsonIgnore
    private List<FollowCompany> followCompanies= new ArrayList<>();

    @OneToMany(mappedBy = "user",
            fetch = FetchType.LAZY,
            cascade = CascadeType.ALL,
            orphanRemoval = true)
    @JsonIgnore
    private List<Notification> notifications= new ArrayList<>();

    @OneToMany(mappedBy = "user",
            fetch = FetchType.LAZY,
            cascade = CascadeType.ALL,
            orphanRemoval = true)
    @JsonIgnore
    private List<JobAlert> jobAlerts= new ArrayList<>();

    @OneToMany(mappedBy = "user",
            fetch = FetchType.LAZY,
            cascade = CascadeType.ALL,
            orphanRemoval = true)
    @JsonIgnore
    private List<TemporaryUpload> temporaryUploads= new ArrayList<>();

    @OneToMany(mappedBy = "user")
    @JsonIgnore
    private List<UserResume> resumes = new ArrayList<>();

    @OneToMany(mappedBy = "user")
    @JsonIgnore
    private List<Application> applications = new ArrayList<>();

    @OneToMany(mappedBy = "user")
    @JsonIgnore
    private List<Subscriber> subscribers = new ArrayList<>();

}
