package com.webjob.application.dto.Request;

import com.webjob.application.enums.CompanyStatus;
import jakarta.persistence.Column;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CompanyDTO {

    @NotBlank(message = "Tên công ty không được để trống")
    @Size(min = 2, max = 255, message = "Tên công ty phải có từ 2 đến 255 ký tự")
    private String name;

    private String description;

    @Size(max = 500, message = "Địa chỉ không được vượt quá 500 ký tự")
    private String address;

    @Size(max = 500, message = "Đường dẫn logo không được vượt quá 500 ký tự")
    private String logo;

    private String website;

    private String email;

    private String phone;

    private Integer employeeSize;

    private Long industryId;

    // Ví dụ sử dụng Validation Annotations trong Spring Boot
    @Min(value = 1800, message = "Năm thành lập không hợp lệ")
    @Max(value = 2027, message = "Năm thành lập không được vượt quá năm hiện tại")
    private Integer foundedYear;

    @Enumerated(EnumType.STRING)
    private CompanyStatus status;

    @Column(unique = true, length = 20)
    private String taxCode;


}
