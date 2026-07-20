package com.webjob.application.dto.Request;

import com.webjob.application.enums.*;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class JobFilterHrRequest {
    /**
     * Search keyword
     */
    private String keyword;

    private JobStatus status;

    private Boolean deleted;

    private Boolean activeOnly;

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

    /**
    /**
     * Negotiable salary
     */
    private Boolean negotiable;


    private JobSort sort;

    private Instant from;
    private Instant to;


}
