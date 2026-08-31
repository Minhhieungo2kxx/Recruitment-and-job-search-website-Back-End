package com.webjob.application.mapper;

import com.webjob.application.document.CompanyDocument;
import com.webjob.application.dto.Response.CompanyAiDetailDTO;
import com.webjob.application.models.Entity.Company;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class CompanyMapper {


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

    public CompanyDocument toDocument(Company company) {
        if (company == null) {
            return null;
        }
        return CompanyDocument.builder()
                .id(company.getId())
                .name(nullToEmpty(company.getName()))
                .description(nullToEmpty(company.getDescription()))
                .address(nullToEmpty(company.getAddress()))
                .logo(nullToEmpty(company.getLogo()))
                .website(nullToEmpty(company.getWebsite()))
                .email(nullToEmpty(company.getEmail()))
                .phone(nullToEmpty(company.getPhone()))
                .employeeSize(company.getEmployeeSize())
                .foundedYear(company.getFoundedYear())
                .status(nullToEmpty(company.getStatus().name()))
                .deleted(company.getDeleted())
                .taxCode(nullToEmpty(company.getTaxCode()))
                .industryId(company.getIndustry() == null ? null : company.getIndustry().getId())
                .industryName(company.getIndustry() == null ? nullToEmpty(null) : nullToEmpty(company.getIndustry().getName()))
                .createdAt(company.getCreatedAt())
                .build();
    }

    private String nullToEmpty(String input) {
        return input == null ? "" : input;
    }
}
