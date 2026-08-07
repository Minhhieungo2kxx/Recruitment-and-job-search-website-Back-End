package com.webjob.application.mapper;

import com.webjob.application.dto.Response.SavedJobResponse;
import com.webjob.application.models.Entity.Job;
import com.webjob.application.models.Entity.SavedJob;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class SavedJobMapper {
    public SavedJobResponse fromEntity(SavedJob savedJob) {
        if (savedJob == null || savedJob.getJob() == null) {
            return null;
        }

        Job job = savedJob.getJob();

        return SavedJobResponse.builder()
                .jobId(job.getId())
                .title(job.getName())
                .companyName(job.getCompany() != null ? job.getCompany().getName() : null)
                .companyLogo(job.getCompany() != null ? job.getCompany().getLogo() : null)
                .location(job.getLocation())
                .salaryMin(job.getSalaryMin() != null ? BigDecimal.valueOf(job.getSalaryMin()) : null)
                .salaryMax(job.getSalaryMax() != null ? BigDecimal.valueOf(job.getSalaryMax()) : null)
                .jobType(job.getWorkingType() != null ? job.getWorkingType().name() : null)
                .deadline(job.getEndDate())
                .savedAt(savedJob.getSavedAt())
                .build();
    }
    public SavedJobResponse fromChatboxAI(SavedJob savedJob) {
        if (savedJob == null || savedJob.getJob() == null) {
            return null;
        }

        Job job = savedJob.getJob();

        return SavedJobResponse.builder()
                .jobId(job.getId())
                .title(job.getName())
                .companyName(job.getCompany() != null ? job.getCompany().getName() : null)
                .location(job.getLocation())
                .salaryMin(job.getSalaryMin() != null ? BigDecimal.valueOf(job.getSalaryMin()) : null)
                .salaryMax(job.getSalaryMax() != null ? BigDecimal.valueOf(job.getSalaryMax()) : null)
                .jobType(job.getWorkingType() != null ? job.getWorkingType().name() : null)
                .deadline(job.getEndDate())
                .build();
    }
}
