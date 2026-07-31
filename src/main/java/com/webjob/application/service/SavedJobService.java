package com.webjob.application.service;

import com.webjob.application.dto.Request.JobFilterAdminRequest;
import com.webjob.application.dto.Response.JobResponse;
import com.webjob.application.dto.Response.MetaDTO;
import com.webjob.application.dto.Response.ResponseDTO;
import com.webjob.application.dto.Response.SavedJobResponse;
import com.webjob.application.exception.Customs.BadRequestException;
import com.webjob.application.exception.Customs.ResourceNotFoundException;
import com.webjob.application.models.Entity.Job;
import com.webjob.application.models.Entity.SavedJob;
import com.webjob.application.models.Entity.User;
import com.webjob.application.repository.JobRepository;
import com.webjob.application.repository.SavedJobRepository;
import com.webjob.application.repository.UserRepository;
import com.webjob.application.utils.common.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SavedJobService {
    private final SavedJobRepository savedJobRepository;

    private final SecurityUtils utils;

    private final UserRepository userRepository;

    private final JobRepository jobRepository;

    @Transactional
    public void saveJob(Long jobId) {

        User user=utils.getCurrentUser();

        if (savedJobRepository.existsByUserIdAndJobId(user.getId(), jobId)) {
            throw new BadRequestException("Job already saved.");
        }

        Job job = jobRepository.findByIdAndDeletedFalse(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("Job not found"));

        SavedJob savedJob = new SavedJob();
        savedJob.setUser(user);
        savedJob.setJob(job);
        savedJobRepository.save(savedJob);
    }
    @Transactional
    public void unsaveJob(Long jobId) {

        Long userId = utils.getCurrentUserId();

        SavedJob savedJob = savedJobRepository.findByUserIdAndJobId(userId, jobId)
                .orElseThrow(() -> new ResourceNotFoundException("Saved job not found"));

        savedJobRepository.delete(savedJob);
    }
    public ResponseDTO<List<SavedJobResponse>> getSavedJobs(int page, int size) {

        if (page <= 0) {
            page = 1;
        }
        if (size <= 0) {
            size = 10;
        }
        Long userId=utils.getCurrentUserId();
        Pageable pageable = PageRequest.of(page-1, size, Sort.by("savedAt").descending());
        Page<SavedJob> pagelist =savedJobRepository.findByUserId(userId,pageable);

        int currentpage = pagelist.getNumber() + 1;
        int pagesize = pagelist.getSize();
        int totalpage = pagelist.getTotalPages();
        Long totalItem = pagelist.getTotalElements();

        MetaDTO metaDTO = new MetaDTO(currentpage, pagesize, totalpage, totalItem);

        List<SavedJobResponse> list = pagelist.getContent().stream()
                .map(this::fromEntity)
                .toList();

        // 4. Trả về kết quả
        return new ResponseDTO<>(metaDTO, list);



    }

    public  SavedJobResponse fromEntity(SavedJob savedJob) {
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
                .salaryMin(BigDecimal.valueOf(job.getSalaryMin()))
                .salaryMax(BigDecimal.valueOf(job.getSalaryMax()))
                .jobType(job.getWorkingType() != null ? job.getWorkingType().name() : null)
                .deadline(job.getEndDate())
                .savedAt(savedJob.getSavedAt())
                .build();
    }
}
