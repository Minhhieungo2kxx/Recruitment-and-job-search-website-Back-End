package com.webjob.application.service;

import com.webjob.application.dto.Request.JobAlertRequest;
import com.webjob.application.dto.Request.Search.SubscriberFilterRequest;
import com.webjob.application.dto.Response.JobAlertResponse;
import com.webjob.application.dto.Response.MetaDTO;
import com.webjob.application.dto.Response.ResponseDTO;
import com.webjob.application.dto.Response.SubscriberListResponse;
import com.webjob.application.enums.AlertFrequency;
import com.webjob.application.exception.Customs.BadRequestException;
import com.webjob.application.exception.Customs.ResourceNotFoundException;
import com.webjob.application.models.Entity.JobAlert;
import com.webjob.application.models.Entity.JobCategory;
import com.webjob.application.models.Entity.Subscriber;
import com.webjob.application.models.Entity.User;
import com.webjob.application.repository.JobAlertRepository;
import com.webjob.application.repository.JobCategoryRepository;
import com.webjob.application.service.Specification.SubscriberSpecification;
import com.webjob.application.utils.common.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.redisson.Redisson;
import org.redisson.RedissonMultiLock;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class JobAlertService {
    private final JobAlertRepository jobAlertRepository;
    private final ModelMapper modelMapper;

    private final SecurityUtils securityUtils;

    private final JobCategoryRepository categoryRepository;
    private final RedissonClient redissonClient;


    @Transactional
    public JobAlertResponse create(JobAlertRequest request) {
        Long userId = securityUtils.getCurrentUserId();

        String keyword = normalize(request.getKeyword());
        String location = normalize(request.getLocation());

        RLock userLock = redissonClient.getLock("job-alert:create:user:" + userId);
        RLock filterLock = redissonClient.getLock(
                        String.format(
                                "job-alert:user:%d:%s:%s:%s:%s:%s",
                                userId, keyword,
                                location, request.getJobCategoryId(),
                                request.getLevel(), request.getWorkMode()
                        )
                );
        RedissonMultiLock multiLock =
                new RedissonMultiLock(userLock, filterLock);

        boolean acquired = false;
        try {
            acquired = multiLock.tryLock(3, 10, TimeUnit.SECONDS);
            if (!acquired) {
                throw new BadRequestException("System is busy. Please retry.");
            }
            if (jobAlertRepository.countByUserId(userId) >= 5) {
                throw new BadRequestException("Maximum number of job alerts reached.");
            }
            validateSalary(request.getSalaryMin(), request.getSalaryMax());
            JobAlert jobAlert = new JobAlert();
            modelMapper.map(request, jobAlert);
            jobAlert.setKeyword(normalize(request.getKeyword() == null ? null : request.getKeyword()));
            jobAlert.setLocation(normalize(request.getLocation() == null ? null : request.getLocation()));

            User user = securityUtils.getCurrentUser();
            jobAlert.setUser(user);

            if (request.getJobCategoryId() != null) {

                JobCategory category = categoryRepository.findById(request.getJobCategoryId())
                        .orElseThrow(() -> new ResourceNotFoundException("JobCategory not found"));
                jobAlert.setJobCategory(category);
            }
            if (jobAlertRepository.existsByUserIdAndKeywordAndLocationAndJobCategoryIdAndLevelAndWorkMode(
                    userId, request.getKeyword()
                    , request.getLocation(), request.getJobCategoryId()
                    , request.getLevel(), request.getWorkMode()
            )) {
                throw new BadRequestException("Job alert already exists.");
            }

            jobAlert.setLastCheckedAt(Instant.now());
            jobAlert.setNextRunAt(calculateCreateNextRunAt(request.getFrequency()));
            JobAlert saved = jobAlertRepository.save(jobAlert);

            JobAlertResponse response = new JobAlertResponse();
            modelMapper.map(saved, response);
            if (saved.getJobCategory() != null) {
                response.setJobCategoryId(saved.getJobCategory().getId());
                response.setJobCategoryName(saved.getJobCategory().getName());
            }

            return response;

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        } finally {
            if (acquired && multiLock.isHeldByCurrentThread()) {
                multiLock.unlock();
            }
        }
    }
    public Instant calculateCreateNextRunAt(AlertFrequency frequency) {
        if (frequency == null) {
            return Instant.now();
        }

        Instant now = Instant.now();
        return switch (frequency) {
            case DAILY -> now.plus(1, ChronoUnit.DAYS);
            case WEEKLY -> now.plus(7, ChronoUnit.DAYS);
            default -> now;
        };
    }

    @Transactional
    public JobAlertResponse update(Long alertId, JobAlertRequest request) {
        Long userId = securityUtils.getCurrentUserId();

        String keyword = normalize(request.getKeyword());
        String location = normalize(request.getLocation());

        RLock jobalertLock =
                redissonClient.getLock("job-alert:update:" + alertId);

        RLock filterLock =
                redissonClient.getLock(
                        String.format(
                                "job-alert:user:%d:%s:%s:%s:%s:%s", userId, keyword,
                                location, request.getJobCategoryId(),
                                request.getLevel(), request.getWorkMode()
                        )
                );
        RedissonMultiLock multiLock =
                new RedissonMultiLock(jobalertLock, filterLock);

        boolean acquired = false;
        try {
            acquired = multiLock.tryLock(3, 10, TimeUnit.SECONDS);

            if (!acquired) {
                throw new BadRequestException("System is busy. Please retry.");
            }
            JobAlert alert = jobAlertRepository.findByIdAndUserId(alertId, userId)
                    .orElseThrow(() -> new ResourceNotFoundException("Job Alert Not found"));
            validateSalary(request.getSalaryMin(), request.getSalaryMax());
            modelMapper.map(request, alert);

            alert.setKeyword(normalize(request.getKeyword()));
            alert.setLocation(normalize(request.getLocation()));
            if (request.getJobCategoryId() != null) {

                JobCategory category = categoryRepository.findById(request.getJobCategoryId())
                        .orElseThrow(() -> new ResourceNotFoundException("JobCategory not found"));
                alert.setJobCategory(category);
            }
            if(request.getFrequency()!=null && request.getFrequency() != alert.getFrequency()){
                alert.setFrequency(request.getFrequency());
                alert.setNextRunAt(calculateCreateNextRunAt(request.getFrequency()));
            }
            alert.setLastCheckedAt(Instant.now());
            JobAlert saved = jobAlertRepository.save(alert);

            JobAlertResponse response = new JobAlertResponse();
            modelMapper.map(saved, response);
            if (saved.getJobCategory() != null) {
                response.setJobCategoryId(saved.getJobCategory().getId());
                response.setJobCategoryName(saved.getJobCategory().getName());
            }
            return response;

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        } finally {
            if (acquired && multiLock.isHeldByCurrentThread()) {
                multiLock.unlock();
            }

        }

    }

    @Transactional
    public void enable(Long alertId) {
        Long userId = securityUtils.getCurrentUserId();
        JobAlert alert = jobAlertRepository.findByIdAndUserId(alertId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Job Alert Not found"));
        if (alert.getActive()) {
            throw new BadRequestException("Job Alert is True");
        }

        alert.setActive(true);
        jobAlertRepository.save(alert);
    }

    @Transactional
    public void disable(Long alertId) {
        Long userId = securityUtils.getCurrentUserId();
        JobAlert alert = jobAlertRepository.findByIdAndUserId(alertId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Job Alert Not found"));
        if (!alert.getActive()) {
            throw new BadRequestException("Job Alert is False");
        }
        alert.setActive(false);
        jobAlertRepository.save(alert);
    }

    public JobAlertResponse getById(Long alertId) {
        Long userId = securityUtils.getCurrentUserId();
        JobAlert alert = jobAlertRepository.findByIdAndUserId(alertId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Job Alert Not found"));
        JobAlertResponse response = new JobAlertResponse();
        modelMapper.map(alert, response);
        if (alert.getJobCategory() != null) {
            response.setJobCategoryId(alert.getJobCategory().getId());
            response.setJobCategoryName(alert.getJobCategory().getName());
        }
        return response;

    }

    @Transactional
    public void delete(Long alertId) {
        Long userId = securityUtils.getCurrentUserId();
        JobAlert alert = jobAlertRepository.findByIdAndUserId(alertId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Job Alert Not found"));
        jobAlertRepository.delete(alert);

    }

    @Transactional(readOnly = true)
    public ResponseDTO<List<JobAlertResponse>> getMyAlerts(int page, int size) {

        size = Math.min(Math.max(size, 1), 50);
        page = Math.max(page, 1);

        Pageable pageable = PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "createdAt"));

        Page<JobAlert> pagelist = jobAlertRepository.findAll(pageable);

        int currentpage = pagelist.getNumber() + 1;
        int pagesize = pagelist.getSize();
        int totalpage = pagelist.getTotalPages();
        Long totalItem = pagelist.getTotalElements();

        MetaDTO metaDTO = new MetaDTO(currentpage, pagesize, totalpage, totalItem);
        List<JobAlertResponse> list = pagelist.getContent().stream()
                .map(alert -> {
                    JobAlertResponse response = new JobAlertResponse();
                    modelMapper.map(alert, response);
                    if (alert.getJobCategory() != null) {
                        response.setJobCategoryId(alert.getJobCategory().getId());
                        response.setJobCategoryName(alert.getJobCategory().getName());
                    }
                    return response;

                })
                .toList();
        // 4. Trả về kết quả
        return new ResponseDTO<>(metaDTO, list);

    }


    private void validateSalary(Double salaryMin, Double salaryMax) {

        if (salaryMin != null
                && salaryMax != null
                && salaryMin > salaryMax) {

            throw new BadRequestException(
                    "Salary minimum cannot be greater than salary maximum.");
        }
    }

    private String normalize(String value) {

        if (value == null) {
            return null;
        }

        value = value.trim();

        return value.isBlank() ? null : value;
    }




}










