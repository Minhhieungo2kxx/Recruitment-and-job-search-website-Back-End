package com.webjob.application.dto.Response;

import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JobCategoryTreeResponse {
    private Long id;

    private String name;

    private Integer level;

    private Long totalJobs;

//    private Integer childrenCount;

    private List<JobCategoryTreeResponse> children = new ArrayList<>();
}
