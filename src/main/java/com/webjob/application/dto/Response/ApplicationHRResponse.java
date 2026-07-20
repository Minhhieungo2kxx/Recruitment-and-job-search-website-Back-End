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
public class ApplicationHRResponse {
    private Long applicationId;

    private String candidateName;

    private String email;

    private String phone;

    private String resumeName;

    private String resumeUrl;

    private Long jobId;

    private String jobName;

    private ResumeStatus status;

    @JsonFormat(
            shape = JsonFormat.Shape.STRING,
            pattern = "yyyy-MM-dd HH:mm:ss a z",
            timezone = "Asia/Ho_Chi_Minh",
            locale = "en_US"
    )
    private Instant appliedAt;
}
