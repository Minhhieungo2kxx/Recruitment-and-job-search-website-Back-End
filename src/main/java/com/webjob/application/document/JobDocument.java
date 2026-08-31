package com.webjob.application.document;

import com.webjob.application.enums.CompanyStatus;
import lombok.*;

import java.time.Instant;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JobDocument {
    private Long id;

    private String name;

    private String location;

    private Double salaryMin;

    private Double salaryMax;

    private Boolean negotiable;

    private Integer quantity;

    private String level;

    private Integer experienceRequired;

    private String workingType;

    private String workMode;

    private String benefits;

    private String requirement;

    private String responsibility;

    private Long viewCount;

    private Integer appliedCount;

    private String competitionLevel;

    private String description;

    private Instant startDate;

    private Instant endDate;

    private String status;

    private Instant createdAt;


    // Soft delete
    private Boolean deleted;


    private Long companyId;
    private String companyName;
    private String companyStatus;
    private Boolean companyDeleted;


    private Long jobCategoryId;
    private String jobCategoryName;

    private List<JobSkillDocument> skills;
}
