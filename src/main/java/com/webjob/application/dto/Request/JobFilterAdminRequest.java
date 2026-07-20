package com.webjob.application.dto.Request;

import com.webjob.application.enums.*;
import jakarta.validation.constraints.Min;
import lombok.*;

import java.time.Instant;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class JobFilterAdminRequest {
    private String keyword;

    private JobStatus status;

    private Boolean deleted;

    /**
     * Danh mục nghề
     */
    private Long jobCategoryId;


    /**
     * Salary
     */
    @Min(0)
    private Double minSalary;

    @Min(0)
    private Double maxSalary;

    @Min(0)
    private Integer experience;

    /**
     * Level
     */
    private List<JobLevel> levels;


    /**
     * Fulltime / Parttime...
     */
    private List<WorkingType> workingTypes;

    /**
     * Remote / Hybrid / Office
     */
    private List<WorkMode> workModes;

    private List<Long> companyIds;

    /**
     /**
     * Negotiable salary
     */
    private Boolean negotiable;


    private Boolean activeOnly;


    private JobSort sort;

    private Instant from;
    private Instant to;

}
