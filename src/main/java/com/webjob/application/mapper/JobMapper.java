package com.webjob.application.mapper;

import com.webjob.application.document.JobDocument;
import com.webjob.application.document.JobSkillDocument;
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

import java.util.List;
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


    private String nullToEmpty(String value) {
        return value != null ? value : "";
    }


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
    public JobDocument toDocument(Job job) {

        if (job == null) {
            return null;
        }

        return JobDocument.builder()
                .id(job.getId())
                .name(job.getName())
                .location(job.getLocation())
                .salaryMin(job.getSalaryMin())
                .salaryMax(job.getSalaryMax())
                .negotiable(job.isNegotiable())
                .quantity(job.getQuantity())
                .level(toString(job.getLevel()))
                .experienceRequired(job.getExperienceRequired())
                .workingType(toString(job.getWorkingType()))
                .workMode(toString(job.getWorkMode()))
                .benefits(job.getBenefits())
                .requirement(job.getRequirement())
                .responsibility(job.getResponsibility())
                .viewCount(job.getViewCount())
                .appliedCount(job.getAppliedCount())
                .competitionLevel(toString(job.getCompetitionLevel()))
                .description(job.getDescription())
                .startDate(job.getStartDate())
                .endDate(job.getEndDate())
                .status(toString(job.getStatus()))
                .createdAt(job.getCreatedAt())
                .deleted(false)
                .companyId(job.getCompany() != null ? job.getCompany().getId() : null)
                .companyName(job.getCompany() != null ? job.getCompany().getName() : null)
                .companyStatus(job.getCompany() != null ? toString(job.getCompany().getStatus()) : null)
                .companyDeleted(job.getCompany() != null ? job.getCompany().getDeleted() : null)
                .jobCategoryId(job.getJobCategory() != null ? job.getJobCategory().getId() : null)
                .jobCategoryName(job.getJobCategory() != null ? job.getJobCategory().getName() : null
                )

                .skills(job.getJobSkills() == null ? List.of() : job.getJobSkills()
                                .stream()
                                .map(this::toSkillDocument)
                                .toList()
                )
                .build();
    }
    private JobSkillDocument toSkillDocument(JobSkill jobSkill) {

        if (jobSkill == null) {
            return null;
        }

        return JobSkillDocument.builder()
                .id(jobSkill.getId())
                .skillId(jobSkill.getSkill() != null ? jobSkill.getSkill().getId() : null)
                .skillName(jobSkill.getSkill() != null ? jobSkill.getSkill().getName() : null)
                .build();
    }

    private String toString(Enum<?> value) {
        return value != null ? value.name() : null;
    }


}
