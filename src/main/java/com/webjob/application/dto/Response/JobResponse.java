package com.webjob.application.dto.Response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.webjob.application.enums.*;
import com.webjob.application.models.Entity.JobCategory;
import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.Instant;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JobResponse {
    private Long id;

    private String name;

    private String location;

    private Double salaryMin;

    private Double salaryMax;

    private boolean negotiable;

    private String currency;

    private Integer quantity;

    private JobLevel level;

    private Integer experienceRequired;



    private WorkingType workingType;

    private WorkMode workMode;

    private String benefits;

    private String requirement;

    private String responsibility;

    private Long viewCount;

    private Integer appliedCount;

    private CompetitionLevel competitionLevel;



    private String description;


    private Instant startDate;


    private Instant endDate;

    private JobStatus status;


    private Instant createdAt;


    private Instant updatedAt;


    private Company company;

    private JobCategory jobCategory;

    private List<JobSkillResponse> skills;


    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Company{

        private Long id;

        private String name;

        private String address;

        private String logo;

    }
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class JobCategory{

        private Long id;

        private String name;
    }
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class JobSkillResponse {

        private Long id;

        private String name;

        private Boolean required;

        private Integer priority;

        private Integer experienceYear;

        private SkillLevel level;
    }


}
