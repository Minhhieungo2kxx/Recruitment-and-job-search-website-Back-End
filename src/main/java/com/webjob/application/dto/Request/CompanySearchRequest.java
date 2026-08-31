package com.webjob.application.dto.Request;

import com.webjob.application.enums.CompanyStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CompanySearchRequest {
    private String keyword;

    private String industry;

    private String taxCode;

    private String email;

    private String phone;

    private String website;

    private String address;


    private Integer minEmployeeSize;

    private Integer maxEmployeeSize;

    private Integer foundedFrom;

    private Integer foundedTo;


}
