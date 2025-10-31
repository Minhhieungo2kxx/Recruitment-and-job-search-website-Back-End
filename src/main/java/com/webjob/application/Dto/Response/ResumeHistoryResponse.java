package com.webjob.application.Dto.Response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.webjob.application.Model.Enums.ResumeStatus;
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
    @JsonFormat(
            shape = JsonFormat.Shape.STRING,
            pattern = "yyyy-MM-dd HH:mm:ss a z",
            timezone = "Asia/Ho_Chi_Minh",
            locale = "en_US"
    )
    private Instant appliedAt;
    private String cvUrl;
    private String salary;
    private ResumeStatus status;

}
