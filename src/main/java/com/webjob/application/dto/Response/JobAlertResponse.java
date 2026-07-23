package com.webjob.application.dto.Response;

import com.webjob.application.enums.AlertFrequency;
import com.webjob.application.enums.JobLevel;
import com.webjob.application.enums.WorkMode;
import lombok.*;

import java.time.Instant;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Setter
public class JobAlertResponse {
    private Long id;

    private String keyword;

    private String location;

    private Double salaryMin;

    private Double salaryMax;

    private Long jobCategoryId;

    private String jobCategoryName;

    private JobLevel level;

    private AlertFrequency frequency;

    private WorkMode workMode;

    private Boolean active;

    private Instant createdAt;

    private Instant lastCheckedAt;
}
