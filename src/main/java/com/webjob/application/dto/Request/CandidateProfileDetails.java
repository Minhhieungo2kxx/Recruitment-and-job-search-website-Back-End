package com.webjob.application.dto.Request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class CandidateProfileDetails {
    @Builder.Default
    private List<Experience> experience = new ArrayList<>();

    @Builder.Default
    private List<Education> education = new ArrayList<>();

    @Builder.Default
    private List<Project> projects = new ArrayList<>();

    @Builder.Default
    private List<Certification> certifications = new ArrayList<>();

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Experience {
        private String company;
        private String title;
        private LocalDate startDate;
        private LocalDate endDate;      // null nếu isCurrent = true
        private boolean isCurrent;
        private String description;
        private Integer durationMonths; // tính sẵn lúc parse, dùng để cộng dồn totalExperienceYears
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Education {
        private String school;
        private String degree;
        private String major;
        private LocalDate startDate;
        private LocalDate endDate;
        private Double gpa;
        /** RELEVANT / PARTIAL / NOT_RELEVANT so với Job.jobCategory — AI gán nhãn, Java tính điểm */
        private String relevance;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Project {
        private String name;
        private String description;
        private String techStack;
        private String role;
        private String relevance; // RELEVANT / PARTIAL / NOT_RELEVANT so với yêu cầu Job
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Certification {
        private String name;
        private String issuer;
        private LocalDate issueDate;
        private LocalDate expireDate;
    }
}
