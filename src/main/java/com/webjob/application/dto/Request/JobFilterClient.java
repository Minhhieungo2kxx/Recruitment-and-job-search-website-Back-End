package com.webjob.application.dto.Request;

import com.webjob.application.enums.*;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.Instant;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class JobFilterClient {

    /**
     * Search keyword
     */
    private String keyword;

    /**
     * Địa điểm
     */
    private String location;

    /**
     * Danh mục nghề
     */
    private Long jobCategoryId;

    /**
     * Company
     */
    private List<Long> companyIds;

    /**
     * Skills
     */
    private List<Long> skillIds;

    /**
     * Salary
     */
    @Min(0)
    private Double minSalary;

    @Min(0)
    private Double maxSalary;

    /**
     * Experience (year)
     */
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
     * Posted Date
     */
    private PostedDateFilter postedDate;

    /**
     * Negotiable salary
     */
    private Boolean negotiable;

    private JobSort sort;


}
