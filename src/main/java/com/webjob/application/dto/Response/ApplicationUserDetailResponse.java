package com.webjob.application.dto.Response;

import com.webjob.application.enums.ResumeStatus;
import com.webjob.application.enums.WorkMode;
import com.webjob.application.enums.WorkingType;
import lombok.*;

import java.time.Instant;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApplicationUserDetailResponse {
    private Long applicationId;

    private ResumeStatus status;

    private Instant appliedAt;


    // Thông tin Job
    private JobInfo job;


    // Thông tin Company
    private CompanyInfo company;


    // CV đã dùng để apply
    private ResumeInfo resume;


    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class JobInfo {

        private Long id;

        private String name;

        private String location;

        private Double salaryMin;

        private Double salaryMax;

        private boolean negotiable;

        private WorkingType workingType;

        private WorkMode workMode;

    }


    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CompanyInfo {

        private Long id;

        private String name;

        private String logo;

    }


    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ResumeInfo {

        private Long id;

        private String name;

        private String url;

    }
}
