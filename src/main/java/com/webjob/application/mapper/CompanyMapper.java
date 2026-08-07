package com.webjob.application.mapper;

import com.webjob.application.dto.Response.CompanyAiDetailDTO;
import com.webjob.application.models.Entity.Company;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class CompanyMapper {

//    public Map<String, Object> toMapDetail(Company company) {
//        if (company == null) {
//            return Map.of();
//        }
//        return Map.ofEntries(
//                Map.entry("id", company.getId()),
//                Map.entry("name", company.getName()),
//                Map.entry("description", nullToEmpty(company.getDescription())),
//                Map.entry("address", nullToEmpty(company.getAddress())),
//                Map.entry("website", nullToEmpty(company.getWebsite())),
//                Map.entry("email", nullToEmpty(company.getEmail())),
//                Map.entry("phone", nullToEmpty(company.getPhone())),
//                Map.entry("foundedYear",company.getFoundedYear()),
//                Map.entry("taxCode",nullToEmpty(company.getTaxCode())),
//                Map.entry("employeeSize", company.getEmployeeSize() != null ? company.getEmployeeSize() : 0),
//                Map.entry("industry", company.getIndustry() != null && company.getIndustry().getName() != null
//                        ? company.getIndustry().getName() : "")
//        );
//    }


    //    private String nullToEmpty(String input) {
//        return input == null ? "" : input;
//    }
    public CompanyAiDetailDTO toCompanyAiDetailDTO(Company company) {
        if (company == null) {
            return null; // Hoặc trả về một DTO rỗng tùy vào business của bạn
        }

        return CompanyAiDetailDTO.builder()
                .id(company.getId())
                .name(company.getName())
                .description(nullToEmpty(company.getDescription()))
                .address(nullToEmpty(company.getAddress()))
                .website(nullToEmpty(company.getWebsite()))
                .email(nullToEmpty(company.getEmail()))
                .phone(nullToEmpty(company.getPhone()))
                .foundedYear(company.getFoundedYear())
                .taxCode(nullToEmpty(company.getTaxCode()))
                .employeeSize(company.getEmployeeSize() != null ? company.getEmployeeSize() : 0)
                .industry(company.getIndustry() != null && company.getIndustry().getName() != null
                        ? company.getIndustry().getName() : "")
                .build();
    }

    private String nullToEmpty(String input) {
        return input == null ? "" : input;
    }
}
