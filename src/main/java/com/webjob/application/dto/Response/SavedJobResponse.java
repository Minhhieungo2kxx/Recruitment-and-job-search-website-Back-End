package com.webjob.application.dto.Response;

import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SavedJobResponse {
    private Long jobId;

    private String title;

    private String companyName;

    private String companyLogo;

    private String location;

    private BigDecimal salaryMin;

    private BigDecimal salaryMax;

    private String jobType;

    private Instant deadline;

    private Instant savedAt;

}
