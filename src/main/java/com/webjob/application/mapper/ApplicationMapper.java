package com.webjob.application.mapper;

import com.webjob.application.dto.Response.*;
import com.webjob.application.messaging.dto.JobAppliedEvent;
import com.webjob.application.models.Entity.*;
import org.springframework.stereotype.Component;

@Component
public class ApplicationMapper {
    public ApplicationResponse toResponseApplication(Application application) {
        if (application == null) {
            return null;
        }

        return ApplicationResponse.builder()
                .id(application.getId())
                .userId(application.getUser().getId())
                .fullName(application.getUser().getFullName())
                .email(application.getEmail())
                .phone(application.getUser().getPhone())
                .jobId(application.getJob().getId())
                .jobName(application.getJob().getName())
                .resumeId(application.getResume().getId())
                .resumeName(application.getResume().getName())
                .resumeUrl(application.getResume().getUrl())
                .status(application.getStatus())
                .createdAt(application.getCreatedAt())
                .updatedAt(application.getUpdatedAt())
                .build();
    }

    public JobAppliedEvent buildJobAppliedEvent(Application application, Job job, User candidate, User recruiter) {
        return JobAppliedEvent.builder()
                // Candidate

                .email(candidate.getEmail())
                .candidateName(candidate.getFullName())
                .appliedAt(application.getCreatedAt())

                // Job
                .jobName(job.getName())

                .location(job.getLocation())

                .salaryMin(job.getSalaryMin())
                .salaryMax(job.getSalaryMax())
                .negotiable(job.isNegotiable())

                .workingType(job.getWorkingType())
                .workMode(job.getWorkMode())
                .level(job.getLevel())

                .startDate(job.getStartDate())
                .endDate(job.getEndDate())

                // Company
                .companyName(job.getCompany().getName())
                .companyLogo(job.getCompany().getLogo())

                // Recruiter
                .hrName(recruiter.getFullName())

                .build();
    }

    public UserResumeResponse toUserResumeResponse(UserResume resume) {

        return UserResumeResponse.builder()
                .id(resume.getId())
                .name(resume.getName())
                .url(resume.getUrl())
                .isDefault(resume.getIsDefault())
                .createdAt(resume.getCreatedAt())
                .build();
    }

    public ApplicationHRResponse toHRResponse(Application application) {
        if (application == null) {
            return null;
        }

        ApplicationHRResponse response = new ApplicationHRResponse();


        response.setApplicationId(application.getId());
        response.setEmail(application.getEmail());
        response.setStatus(application.getStatus());
        response.setAppliedAt(application.getCreatedAt());


        if (application.getUser() != null) {
            response.setCandidateName(application.getUser().getFullName());
            response.setPhone(application.getUser().getPhone());
        }

        // Map thông tin từ thực thể Job (Null-safe)
        if (application.getJob() != null) {
            response.setJobId(application.getJob().getId());
            // Thay thế "getTitle()" bằng getter thực tế trong class Job của bạn
            response.setJobName(application.getJob().getName());
        }

        // Map thông tin từ thực thể UserResume (Null-safe)
        if (application.getResume() != null) {
            response.setResumeName(application.getResume().getName()); // Thay thế bằng getter thực tế
            response.setResumeUrl(application.getResume().getUrl());   // Thay thế bằng getter thực tế
        }

        return response;
    }

    public ResumeHistoryResponse resumeHistoryResponse(Application application) {
        if (application == null) {
            return null;
        }
        return ResumeHistoryResponse.builder()
                .companyName(application.getJob().getCompany().getName())
                .companyLogo(application.getJob().getCompany().getLogo())
                .jobName(application.getJob().getName())
                .appliedAt(application.getCreatedAt())
                .cvUrl(application.getResume().getUrl())
                .salaryMin(application.getJob().getSalaryMin())
                .salaryMax(application.getJob().getSalaryMax())
                .negotiable(application.getJob().isNegotiable())
                .status(application.getStatus())
                .build();
    }

    public ApplicationUserDetailResponse toApplicationUserDetailResponse(Application application) {
        if (application == null) {
            return null;
        }
        Job job = application.getJob();

        Company company = job.getCompany();
        UserResume resume = application.getResume();

        return ApplicationUserDetailResponse.builder()
                .applicationId(application.getId())
                .status(application.getStatus())
                .appliedAt(application.getCreatedAt())
                .job(
                        ApplicationUserDetailResponse.JobInfo.builder()
                                .id(job.getId())
                                .name(job.getName())
                                .location(job.getLocation())
                                .salaryMin(job.getSalaryMin())
                                .salaryMax(job.getSalaryMax())
                                .negotiable(job.isNegotiable())
                                .workingType(job.getWorkingType())
                                .workMode(job.getWorkMode())
                                .build()
                )
                .company(
                        ApplicationUserDetailResponse.CompanyInfo.builder()
                                .id(company.getId())
                                .name(company.getName())
                                .logo(company.getLogo())
                                .build()
                )
                .resume(
                        ApplicationUserDetailResponse.ResumeInfo.builder()
                                .id(resume.getId())
                                .name(resume.getName())
                                .url(resume.getUrl())
                                .build()
                )

                .build();
    }

    public ApplicationHrDetailResponse toApplicationHrDetailResponse(Application application) {
        if (application == null) {
            return null;
        }

        User candidate = application.getUser();
        UserResume resume = application.getResume();
        Job job = application.getJob();

        return ApplicationHrDetailResponse.builder()
                .applicationId(application.getId())

                .status(application.getStatus())

                .appliedAt(application.getCreatedAt())

                .candidate(
                        ApplicationHrDetailResponse.CandidateInfo.builder()
                                .id(candidate.getId())
                                .fullName(candidate.getFullName())
                                .email(candidate.getEmail())
                                .phone(candidate.getPhone())
                                .avatar(candidate.getAvatar())
                                .dateOfBirth(candidate.getDateOfBirth())
                                .gender(candidate.getGender())
                                .address(candidate.getAddress())
                                .build()
                )

                .resume(
                        ApplicationHrDetailResponse.ResumeInfo.builder()
                                .id(resume.getId())
                                .name(resume.getName())
                                .url(resume.getUrl())
                                .build()
                )

                .job(
                        ApplicationHrDetailResponse.JobInfo.builder()
                                .id(job.getId())
                                .name(job.getName())
                                .location(job.getLocation())
                                .salaryMin(job.getSalaryMin())
                                .salaryMax(job.getSalaryMax())
                                .build()
                )

                .build();

    }
    public AdminApplicationResponse mapAdminApplication(Application a){
        if(a==null){
            return null;
        }
        return AdminApplicationResponse.builder()

                .applicationId(a.getId())

                .status(a.getStatus())

                .appliedAt(a.getCreatedAt())


                .candidate(
                        AdminApplicationResponse.CandidateInfo.builder()
                                .id(a.getUser().getId())
                                .fullName(a.getUser().getFullName())
                                .email(a.getUser().getEmail())
                                .build()
                )


                .job(
                        AdminApplicationResponse.JobInfo.builder()
                                .id(a.getJob().getId())
                                .name(a.getJob().getName())
                                .build()
                )


                .company(
                        AdminApplicationResponse.CompanyInfo.builder()
                                .id(a.getJob()
                                        .getCompany()
                                        .getId())
                                .name(a.getJob()
                                        .getCompany()
                                        .getName())
                                .build()
                )

                .build();
    }
}
