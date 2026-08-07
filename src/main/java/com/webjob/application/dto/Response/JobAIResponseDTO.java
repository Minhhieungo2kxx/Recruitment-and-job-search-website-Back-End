package com.webjob.application.dto.Response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JobAIResponseDTO {
    private Long id;
    private String name;
    private String company;
    private String location;
    private Double salaryMin;
    private Double salaryMax;
    private Integer experienceRequired;
    private String workMode;
    private String workingType;
    private String level;
    private String category;
}
