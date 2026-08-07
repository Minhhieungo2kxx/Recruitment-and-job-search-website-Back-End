package com.webjob.application.dto.Response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CompanyAiDetailDTO {
    private Long id; // Hoặc kiểu dữ liệu tương ứng với ID của bạn
    private String name;
    private String description;
    private String address;
    private String website;
    private String email;
    private String phone;
    private Integer foundedYear;
    private String taxCode;
    private Integer employeeSize;
    private String industry;
}
