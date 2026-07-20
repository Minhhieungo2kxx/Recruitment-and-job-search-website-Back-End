package com.webjob.application.dto.Request;

import com.webjob.application.enums.CategoryStatus;
import com.webjob.application.enums.SkillLevel;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class JobCategoryRequest {
    @NotBlank
    private String name;

    private String description;

//    private Integer level;

    private CategoryStatus status;

    private Long parentId;

    private List<JobCategorySkillRequest> skills;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class JobCategorySkillRequest{
        private Long skillId;

        private SkillLevel level;

        private Boolean required;

        private Integer weight;

    }
}
