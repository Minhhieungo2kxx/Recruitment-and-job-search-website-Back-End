package com.webjob.application.dto.Response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class JobRecommendationContext {
    private Long jobId;
    private String jobName;
    private String companyName;
    private String location;
    private String level;
    private String workMode;
    private String workingType;
    private Double salaryMin;
    private Double salaryMax;


    private Integer score;
    private List<String> matchedSkills;
    private List<String> reasons;
}
