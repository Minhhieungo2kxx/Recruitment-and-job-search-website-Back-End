package com.webjob.application.dto.Response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.webjob.application.enums.ResumeStatus;
import lombok.*;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApplicationResponse {
    private Long id;

    // Thông tin ứng viên
    private Long userId;
    private String fullName;
    private String email;
    private String phone;

    // Thông tin Job
    private Long jobId;
    private String jobName;

    // Thông tin CV
    private Long resumeId;
    private String resumeName;
    private String resumeUrl;

    // Trạng thái
    private ResumeStatus status;

    @JsonFormat(
            shape = JsonFormat.Shape.STRING,
            pattern = "yyyy-MM-dd HH:mm:ss a z",
            timezone = "Asia/Ho_Chi_Minh",
            locale = "en_US"
    )
    private Instant createdAt;

    @JsonFormat(
            shape = JsonFormat.Shape.STRING,
            pattern = "yyyy-MM-dd HH:mm:ss a z",
            timezone = "Asia/Ho_Chi_Minh",
            locale = "en_US"
    )
    private Instant updatedAt;
}
