package com.webjob.application.messaging.dto;

import com.webjob.application.enums.JobLevel;
import com.webjob.application.enums.WorkMode;
import com.webjob.application.enums.WorkingType;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JobAppliedEvent {
    // Candidate

    private String email;
    private String candidateName;
    private Instant appliedAt;

    // Job
    private String jobName;
    private String location;

    private Double salaryMin;
    private Double salaryMax;
    private boolean negotiable;

    private WorkingType workingType;
    private WorkMode workMode;
    private JobLevel level;

    private Instant startDate;
    private Instant endDate;

    // Company
    private String companyName;
    private String companyLogo;

    // Recruiter
    private String hrName;

}
