package com.webjob.application.dto.Response;

import com.webjob.application.enums.ResumeStatus;
import lombok.*;

import java.time.Instant;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminApplicationResponse {
    private Long applicationId;


    // trạng thái apply
    private ResumeStatus status;


    private Instant appliedAt;



    // User
    private CandidateInfo candidate;



    // Job
    private JobInfo job;



    // Company
    private CompanyInfo company;



    @Getter
    @Setter
    @Builder
    public static class CandidateInfo {

        private Long id;

        private String fullName;

        private String email;

    }



    @Getter
    @Setter
    @Builder
    public static class JobInfo {

        private Long id;

        private String name;

    }



    @Getter
    @Setter
    @Builder
    public static class CompanyInfo {

        private Long id;

        private String name;

    }
}
