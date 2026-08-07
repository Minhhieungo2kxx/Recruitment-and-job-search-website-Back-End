package com.webjob.application.dto.Response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
@Builder
public class GeminiJobResponAI {
    private List<String> userSkills;
    private List<JobRecommendationContext> jobs;
}
