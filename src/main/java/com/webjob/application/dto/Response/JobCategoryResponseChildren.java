package com.webjob.application.dto.Response;

import com.webjob.application.enums.CategoryStatus;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JobCategoryResponseChildren {
    private Long id;

    private String name;

    private String description;

    private Integer level;

    private CategoryStatus status;

    private Long parentId;

    private Integer childrenCount;

    @Builder.Default
    private List<JobCategoryResponseChildren> children = new ArrayList<>();
}
