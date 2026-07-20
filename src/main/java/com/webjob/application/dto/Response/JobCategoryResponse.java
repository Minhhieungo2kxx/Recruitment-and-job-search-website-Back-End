package com.webjob.application.dto.Response;

import com.webjob.application.enums.CategoryStatus;
import com.webjob.application.enums.SkillLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class JobCategoryResponse {
    private Long id;

    private String name;

    private String description;

    private Integer level;

    private CategoryStatus status;

    private Long parentId;

//    private Integer childrenCount;

    private List<JobCategorySkillResponse> skills;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class JobCategorySkillResponse {
        private Long id;

        private Long skillId;

        private String skillName;

        private SkillLevel level;

        private Boolean required;

        private Integer weight;

    }
}
