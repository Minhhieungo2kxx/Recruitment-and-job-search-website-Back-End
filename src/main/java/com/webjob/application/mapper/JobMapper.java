package com.webjob.application.mapper;

import com.webjob.application.dto.Response.JobAIDetailResponseDTO;
import com.webjob.application.dto.Response.JobAIResponseDTO;
import com.webjob.application.dto.Response.JobResponse;
import com.webjob.application.enums.JobSort;
import com.webjob.application.models.Entity.Job;
import com.webjob.application.models.Entity.JobSkill;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class JobMapper {
    private final ModelMapper modelMapper;

    public JobResponse toResponse(Job job) {
        if (job == null) {
            return null;
        }

        JobResponse response = modelMapper.map(job, JobResponse.class);


        if (job.getCompany() != null) {
            response.setCompany(
                    JobResponse.Company.builder()
                            .id(job.getCompany().getId())
                            .name(job.getCompany().getName())
                            .address(job.getCompany().getAddress())
                            .logo(job.getCompany().getLogo())
                            .build()
            );
        }


        if (job.getJobCategory() != null) {
            response.setJobCategory(
                    JobResponse.JobCategory.builder()
                            .id(job.getJobCategory().getId())
                            .name(job.getJobCategory().getName())
                            .build()
            );
        }


        response.setSkills(
                job.getJobSkills()
                        .stream()
                        .map(js -> JobResponse.JobSkillResponse.builder()
                                .id(js.getSkill().getId())
                                .name(js.getSkill().getName())
                                .required(js.getRequired())
                                .priority(js.getPriority())
                                .experienceYear(js.getExperienceYear())
                                .level(js.getLevel())
                                .build())
                        .toList()
        );

        return response;
    }

    public Sort toSort(JobSort sort) {
        if (sort == null) {
            return Sort.by(
                    Sort.Direction.DESC,
                    "createdAt"
            );
        }
        return switch (sort) {
            case NEWEST -> Sort.by(Sort.Direction.DESC, "createdAt");

            case SALARY_HIGH -> Sort.by(Sort.Direction.DESC, "salaryMax");

            case SALARY_LOW -> Sort.by(Sort.Direction.ASC, "salaryMin");

            case MOST_VIEWED -> Sort.by(Sort.Direction.DESC, "viewCount");

            case LESS_COMPETITION -> Sort.by(Sort.Direction.ASC, "appliedCount");

            case EXPIRING_SOON -> Sort.by(Sort.Direction.ASC, "endDate"
            );
        };
    }
//    public Map<String, Object> detailJobForAI(Job job, Object skills) {
//        if (job == null) {
//            return null;
//        }
//
//        return Map.ofEntries(
//                Map.entry("id", job.getId()),
//                Map.entry("name", job.getName()),
//                Map.entry("company", job.getCompany() != null ? job.getCompany().getName() : ""),
//                Map.entry("location", job.getLocation()),
//                Map.entry("salaryMin", job.getSalaryMin()),
//                Map.entry("salaryMax", job.getSalaryMax()),
//                Map.entry("negotiable", job.isNegotiable()),
//                Map.entry("level", job.getLevel() != null ? job.getLevel().name() : ""),
//                Map.entry("workMode", job.getWorkMode() != null ? job.getWorkMode().name() : ""),
//                Map.entry("workingType", job.getWorkingType() != null ? job.getWorkingType().name() : ""),
//                Map.entry("experienceRequired", job.getExperienceRequired()),
//                Map.entry("requirement", nullToEmpty(job.getRequirement())),
//                Map.entry("responsibility", nullToEmpty(job.getResponsibility())),
//                Map.entry("benefits", nullToEmpty(job.getBenefits())),
//                Map.entry("description", nullToEmpty(job.getDescription())),
//                Map.entry("skills", skills),
//                Map.entry("status", job.getStatus() != null ? job.getStatus().name() : "")
//        );
//    }

    private String nullToEmpty(String value) {
        return value != null ? value : "";
    }

    //    public Map<String, Object> searchJobAI(Job job) {
//        if (job == null) {
//            return null;
//        }
//        return Map.ofEntries(
//                Map.entry("id", job.getId()),
//                Map.entry("name", job.getName()),
//                Map.entry("company", job.getCompany() != null ? job.getCompany().getName() : ""),
//                Map.entry("location", job.getLocation()),
//                Map.entry("salaryMin", job.getSalaryMin()),
//                Map.entry("salaryMax", job.getSalaryMax()),
//                Map.entry("experienceRequired", job.getExperienceRequired()),
//                Map.entry("workMode", job.getWorkMode() != null ? job.getWorkMode().name() : ""),
//                Map.entry("workingType", job.getWorkingType() != null ? job.getWorkingType().name() : ""),
//                Map.entry("level", job.getLevel() != null ? job.getLevel().name() : ""),
//                Map.entry("category", job.getJobCategory() != null ? job.getJobCategory().getName() : "")
//        );
//    }
    public JobAIResponseDTO searchJobAI(Job job) {
        if (job == null) {
            return null;
        }
        return JobAIResponseDTO.builder()
                .id(job.getId())
                .name(job.getName())
                .company(job.getCompany() != null ? job.getCompany().getName() : "")
                .location(job.getLocation())
                .salaryMin(job.getSalaryMin())
                .salaryMax(job.getSalaryMax())
                .experienceRequired(job.getExperienceRequired())
                .workMode(job.getWorkMode() != null ? job.getWorkMode().name() : "")
                .workingType(job.getWorkingType() != null ? job.getWorkingType().name() : "")
                .level(job.getLevel() != null ? job.getLevel().name() : "")
                .category(job.getJobCategory() != null ? job.getJobCategory().getName() : "")
                .build();
    }
    public JobAIDetailResponseDTO detailJobForAI(Job job, Object skills) {
        if (job == null) {
            return null;
        }

        return JobAIDetailResponseDTO.builder()
                .id(job.getId())
                .name(job.getName())
                .company(job.getCompany() != null ? job.getCompany().getName() : "")
                .location(job.getLocation())
                .salaryMin(job.getSalaryMin())
                .salaryMax(job.getSalaryMax())
                .negotiable(job.isNegotiable())
                .level(job.getLevel() != null ? job.getLevel().name() : "")
                .workMode(job.getWorkMode() != null ? job.getWorkMode().name() : "")
                .workingType(job.getWorkingType() != null ? job.getWorkingType().name() : "")
                .experienceRequired(job.getExperienceRequired())
                .requirement(nullToEmpty(job.getRequirement()))
                .responsibility(nullToEmpty(job.getResponsibility()))
                .benefits(nullToEmpty(job.getBenefits()))
                .description(nullToEmpty(job.getDescription()))
                .skills(skills)
                .status(job.getStatus() != null ? job.getStatus().name() : "")
                .build();
    }


}
