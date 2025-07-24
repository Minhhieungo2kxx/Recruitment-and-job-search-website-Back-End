package com.webjob.application.Models;


import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.List;

@Entity
@Table(name = "companies")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)

public class Company {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


    @Column(nullable = false)
    @NotBlank(message = "Tên công ty không được để trống")
    @Size(min = 2, max = 255, message = "Tên công ty phải có từ 2 đến 255 ký tự")
    private String name;

    @Column(columnDefinition = "MEDIUMTEXT")
    private String description;

    @Size(max = 500, message = "Địa chỉ không được vượt quá 500 ký tự")
    private String address;

    @Size(max = 500, message = "Đường dẫn logo không được vượt quá 500 ký tự")
    private String logo;


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

    @OneToMany(mappedBy ="company",fetch =FetchType.LAZY)
    @JsonIgnore
    private List<User> users;


}
