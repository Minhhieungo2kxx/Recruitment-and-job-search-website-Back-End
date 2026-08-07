package com.webjob.application.dto.Response;

import com.webjob.application.enums.ResumeStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AppliedJobResponseAIDTO {
    private Long jobId;
    private String jobName;
    private String company;
    private ResumeStatus status;
    private Instant appliedAt;
}
