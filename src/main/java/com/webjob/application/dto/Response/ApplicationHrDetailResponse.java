package com.webjob.application.dto.Response;

import com.webjob.application.enums.ResumeStatus;
import lombok.*;

import java.time.Instant;
import java.time.LocalDate;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApplicationHrDetailResponse {
    private Long applicationId;


    private ResumeStatus status;


    private Instant appliedAt;


    // Người ứng tuyển
    private CandidateInfo candidate;


    // CV ứng viên gửi
    private ResumeInfo resume;


    // Job mà HR đăng
    private JobInfo job;


    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CandidateInfo {

        private Long id;

        private String fullName;

        private String email;

        private String phone;

        private String avatar;

        private LocalDate dateOfBirth;

        private String gender;

        private String address;
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

    }
}
