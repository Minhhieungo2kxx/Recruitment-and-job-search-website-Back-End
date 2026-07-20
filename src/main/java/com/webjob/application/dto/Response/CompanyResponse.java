package com.webjob.application.dto.Response;

import com.webjob.application.enums.CompanyStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CompanyResponse {
    private Long id;

    private String name;

    private String description;

    private String address;

    private String logo;

    private String website;

    private String email;

    private String phone;

    private Integer employeeSize;

    private String industry;

    private Integer foundedYear;

    private String taxCode;

    private CompanyStatus status;

    private Instant createdAt;

    private Instant updatedAt;


    private Boolean deleted;

    private Instant deletedAt;



    // Thống kê
    private Integer jobCount;


    private Integer followerCount;
}
