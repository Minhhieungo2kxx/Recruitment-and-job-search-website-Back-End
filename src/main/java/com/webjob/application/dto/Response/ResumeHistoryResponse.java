package com.webjob.application.dto.Response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.webjob.application.enums.ResumeStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ResumeHistoryResponse {
    private String companyName;
    private String jobName;
    private String companyLogo;
    private Instant appliedAt;
    private String cvUrl;
    private Double salaryMin;
    private Double salaryMax;
    private boolean negotiable;
    private ResumeStatus status;

}
