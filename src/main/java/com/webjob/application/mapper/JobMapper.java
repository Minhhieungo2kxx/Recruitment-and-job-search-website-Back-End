package com.webjob.application.mapper;

import com.webjob.application.dto.Response.JobResponse;
import com.webjob.application.enums.JobSort;
import com.webjob.application.models.Entity.Job;
import com.webjob.application.models.Entity.JobSkill;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

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

            case NEWEST ->
                    Sort.by(
                            Sort.Direction.DESC,
                            "createdAt"
                    );


            case SALARY_HIGH ->
                    Sort.by(
                            Sort.Direction.DESC,
                            "salaryMax"
                    );


            case SALARY_LOW ->
                    Sort.by(
                            Sort.Direction.ASC,
                            "salaryMin"
                    );


            case MOST_VIEWED ->
                    Sort.by(
                            Sort.Direction.DESC,
                            "viewCount"
                    );


            case LESS_COMPETITION ->
                    Sort.by(
                            Sort.Direction.ASC,
                            "appliedCount"
                    );


            case EXPIRING_SOON ->
                    Sort.by(
                            Sort.Direction.ASC,
                            "endDate"
                    );
        };
    }
}
