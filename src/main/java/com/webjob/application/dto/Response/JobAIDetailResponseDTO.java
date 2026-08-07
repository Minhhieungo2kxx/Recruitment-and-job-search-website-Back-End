package com.webjob.application.dto.Response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JobAIDetailResponseDTO {
    private Long id;
    private String name;
    private String company;
    private String location;
    private Double salaryMin;
    private Double salaryMax;
    private boolean negotiable;
    private String level;
    private String workMode;
    private String workingType;
    private Integer experienceRequired;
    private String requirement;
    private String responsibility;
    private String benefits;
    private String description;
    private Object skills;
    private String status;
}
