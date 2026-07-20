package com.webjob.application.dto.Request;

import com.webjob.application.enums.CompanyStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CompanyAdminSearchRequest {
    private String keyword;

    private String industry;

    private List<CompanyStatus> statuses;

    private Boolean deleted;

    private String taxCode;

    private String email;

    private String phone;

    private Integer foundedFrom;

    private Integer foundedTo;

    private Integer minEmployeeSize;

    private Integer maxEmployeeSize;

    private Instant createdFrom;

    private Instant createdTo;


}
