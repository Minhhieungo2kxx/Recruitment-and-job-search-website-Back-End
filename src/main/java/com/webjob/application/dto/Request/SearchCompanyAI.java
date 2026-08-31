package com.webjob.application.dto.Request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SearchCompanyAI {
    private String name;
    private String taxCode;
    private String email;
    private String phone;
    private String website;
    private String address;
    private String industryName;
}
