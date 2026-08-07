package com.webjob.application.service;

import com.webjob.application.dto.Request.JobFilterAdminRequest;
import com.webjob.application.dto.Response.JobResponse;
import com.webjob.application.dto.Response.MetaDTO;
import com.webjob.application.dto.Response.ResponseDTO;
import com.webjob.application.dto.Response.SavedJobResponse;
import com.webjob.application.exception.Customs.*;
import com.webjob.application.mapper.SavedJobMapper;
import com.webjob.application.models.Entity.Job;
import com.webjob.application.models.Entity.SavedJob;
import com.webjob.application.models.Entity.User;
import com.webjob.application.repository.JobRepository;
import com.webjob.application.repository.SavedJobRepository;
import com.webjob.application.repository.UserRepository;
import com.webjob.application.utils.common.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class SavedJobService {
    private final SavedJobRepository savedJobRepository;

    private final SecurityUtils utils;

    private final UserRepository userRepository;

    private final JobRepository jobRepository;

    private final SavedJobMapper savedJobMapper;
    private final RedissonClient redissonClient;

    @Transactional
    public void saveJob(Long jobId) {
        User user = utils.getCurrentUser();

        log.info("User {} saved job {}", user.getId(), jobId);

        RLock lock = redissonClient.getLock(buildSaveJobLock(user.getId(), jobId));
        boolean acquired = false;

        try {
            acquired = lock.tryLock(5, 30, TimeUnit.SECONDS);

            if (!acquired) {
                throw new ResourceLockedException(
                        "Bạn đang thực hiện thao tác lưu công việc yêu thích. Vui lòng thử lại."
                );
            }

            Job job = jobRepository.findByIdAndDeletedFalse(jobId)
                    .orElseThrow(() -> new ResourceNotFoundException("Job not found"));

            SavedJob savedJob = new SavedJob();
            savedJob.setUser(user);
            savedJob.setJob(job);

            savedJobRepository.save(savedJob);

        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new AppException("Không thể lấy lock."+ex);

        } catch (DataIntegrityViolationException ex) {
            log.info("Duplicate saveJob. userId={}, jobId={}", user.getId(), jobId, ex);
            throw new BadRequestException("Bạn đã lưu công việc này.");

        } finally {
            if (acquired && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
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
                .map(savedJobMapper::fromEntity)
                .toList();

        // 4. Trả về kết quả
        return new ResponseDTO<>(metaDTO, list);



    }


    private String buildSaveJobLock(Long userId, Long jobId) {
        return "lock:saveJob:" + userId + ":" + jobId;
    }

}
