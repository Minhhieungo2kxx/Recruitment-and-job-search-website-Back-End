package com.webjob.application.service;

import com.webjob.application.dto.Request.ApplyRequest;
import com.webjob.application.dto.Request.UpdateApplicationStatusRequest;
import com.webjob.application.dto.Response.*;
import com.webjob.application.enums.ResumeStatus;
import com.webjob.application.exception.Customs.*;
import com.webjob.application.mapper.ApplicationMapper;
import com.webjob.application.models.Entity.*;
import com.webjob.application.repository.ApplicationRepository;
import com.webjob.application.repository.JobRepository;
import com.webjob.application.repository.TemporaryUploadRepository;
import com.webjob.application.repository.UserResumeRepository;
import com.webjob.application.service.Specification.ApplicationSpecification;
import com.webjob.application.utils.common.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.AccessDeniedException;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.TimeUnit;


@Service
@RequiredArgsConstructor
@Slf4j
public class ApplicationService {

    private final JobService jobService;

    private final UserService userService;


    private final TemporaryUploadRepository temporaryUploadRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final RedissonClient redissonClient;
    private final ApplicationRepository applicationRepository;

    private final JobRepository jobRepository;

    private final UserResumeRepository userResumeRepository;

    private final ApplicationMapper applicationMapper;

    private final SecurityUtils securityUtils;

    @Transactional
    public ApplicationResponse apply(ApplyRequest request) {
        User user = securityUtils.getCurrentUser();
        Job job = jobService.getById(request.getJobId());

        User hr = userService.getbyHR(job.getCompany());
        log.info("User {} applied job {}", user.getId(), job.getId());
        RLock lock = redissonClient.getLock(
                buildApplyLock(user.getId(), job.getId())
        );
        boolean acquired = false;
        try {
            acquired = lock.tryLock(5, TimeUnit.SECONDS);
            if (!acquired) {
                log.warn("Cannot acquire apply lock. userId={}, jobId={}", user.getId(), job.getId());
                throw new ResourceLockedException("Bạn đang thực hiện thao tác ứng tuyển. Vui lòng thử lại.");
            }
            if (job.getEndDate().isBefore(Instant.now())) {
                throw new JobExpiredException("Công việc đã hết hạn.");
            }
            if (applicationRepository.existsByUserIdAndJobId(user.getId(), job.getId())) {
                throw new AlreadyAppliedException("Bạn đã ứng tuyển.");
            }

            UserResume resume = resolveResume(request, user);
            Application application = Application.builder()
                    .email(user.getEmail())
                    .status(ResumeStatus.PENDING)
                    .user(user)
                    .job(job)
                    .resume(resume)
                    .build();

            Application saved = applicationRepository.save(application);

            jobRepository.increaseAppliedCount(job.getId());

//            send email
            eventPublisher.publishEvent(applicationMapper.buildJobAppliedEvent(saved, job, user, hr));
            log.info("Apply success. resumeId={}, userId={}, jobId={}", saved.getId(), user.getId(), job.getId());
            return applicationMapper.toResponseApplication(saved);

        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new AppException("Không thể lấy lock. " + ex.getMessage());
        } catch (DataIntegrityViolationException ex) {
            log.info("Duplicate apply. userId={}, jobId={}", user.getId(), job.getId()
            );
            throw new AlreadyAppliedException("Bạn đã ứng tuyển công việc này.");

        } finally {

            if (acquired && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }

        }
    }

    private String buildApplyLock(Long userId, Long jobId) {
        return "lock:apply:" + userId + ":" + jobId;
    }

    private UserResume resolveResume(ApplyRequest request, User user) {

        if (request.getResumeId() != null) {

            return userResumeRepository.findByIdAndUserId(request.getResumeId(), user.getId())
                    .orElseThrow(() -> new AppException("CV không tồn tại."));
        }

        TemporaryUpload upload = temporaryUploadRepository
                .findByPublicIdAndUsedFalse(request.getPublicId())
                .orElseThrow(() -> new BadRequestException("File không tồn tại hoặc đã được sử dụng."));

        if (!upload.getUser().getId().equals(user.getId())) {
            throw new AppException("CV không thuộc người dùng.");
        }
        long count = userResumeRepository.countByUserId(user.getId());
        if (count >= 5) {
            throw new BadRequestException("Bạn chỉ được lưu tối đa 5 CV.");
        }
        boolean isDefault = count == 0 || Boolean.TRUE.equals(request.getIsDefault());

        if (isDefault) {
            userResumeRepository.clearDefaultResume(user.getId());
        }

        UserResume resume = UserResume.builder()
                .name("CV " + LocalDate.now())
                .url(upload.getUrl())
                .publicId(upload.getPublicId())
                .resourceType(upload.getResourceType())
                .isDefault(request.getIsDefault())
                .user(user).build();
        UserResume savedResume = userResumeRepository.save(resume);
        upload.setUsed(true);
        temporaryUploadRepository.save(upload);

        return savedResume;
    }

    public Page<Application> getAllResumeHRCompany(int page, int size) {

        User hr = securityUtils.getCurrentUser();

        if (hr.getCompany() == null) {
            throw new BadRequestException("HR chưa thuộc công ty nào.");
        }

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));

        return applicationRepository.findApplicationsByCompany(hr.getCompany().getId(), pageable);
    }

    public ResponseDTO<List<ApplicationHRResponse>> listResumeHRCompany(int page, int size) {
        // Bỏ hoàn toàn try-catch, chỉ giữ lại logic kiểm tra số âm/bằng 0
        page = Math.max(page, 1);
        size = Math.min(Math.max(size, 1), 50);
        Page<Application> pagelist = getAllResumeHRCompany(page - 1, size);
        return convertToResumeHRCompany(pagelist);

    }

    public ResponseDTO<List<ResumeHistoryResponse>> getHistoryAppliedClient(int page, int size, ResumeStatus status) {
        // Bỏ hoàn toàn try-catch, chỉ giữ lại logic kiểm tra số âm/bằng 0
        page = Math.max(page, 1);
        size = Math.min(Math.max(size, 1), 50);

        Long useId = securityUtils.getCurrentUserId();
        Pageable pageable = PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "createdAt"));

        Specification<Application> spec =
                Specification.where(ApplicationSpecification.hasUserId(useId))
                        .and(ApplicationSpecification.hasStatus(status));

        Page<Application> pagelist = applicationRepository.findAll(spec, pageable);
        int currentpage = pagelist.getNumber() + 1;
        int pagesize = pagelist.getSize();
        int totalpage = pagelist.getTotalPages();
        Long totalItem = pagelist.getTotalElements();

        MetaDTO metaDTO = new MetaDTO(currentpage, pagesize, totalpage, totalItem);

        List<Application> applications = pagelist.getContent();
        List<ResumeHistoryResponse> list = applications.stream()
                .map(applicationMapper::resumeHistoryResponse)
                .toList();
        // 4. Trả về kết quả
        return new ResponseDTO<>(metaDTO, list);

    }

    public ResponseDTO<List<ApplicationHRResponse>> convertToResumeHRCompany(Page<Application> pagelist) {

        int currentpage = pagelist.getNumber() + 1;
        int pagesize = pagelist.getSize();
        int totalpage = pagelist.getTotalPages();
        Long totalItem = pagelist.getTotalElements();

        MetaDTO metaDTO = new MetaDTO(currentpage, pagesize, totalpage, totalItem);

        List<Application> applications = pagelist.getContent();
        List<ApplicationHRResponse> list = applications.stream()
                .map(applicationMapper::toHRResponse)
                .toList();
        // 4. Trả về kết quả
        return new ResponseDTO<>(metaDTO, list);
    }

    @Transactional
    public ApplicationResponse updateStatusByHr(Long applicationId, UpdateApplicationStatusRequest request) {
        Application application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy hồ sơ ứng tuyển"));

        User hr = securityUtils.getCurrentUser();
        Company company = hr.getCompany();
        if (company == null) {
            throw new BadRequestException("HR chưa thuộc công ty nào.");
        }

        if (!application.getJob().getCompany().getId().equals(company.getId())) {
            throw new ForbiddenException("Bạn không có quyền cập nhật hồ sơ này.");
        }

        ResumeStatus current = application.getStatus();

        validateStatusTransition(current, request.getStatus());

        application.setStatus(request.getStatus());
        return applicationMapper.toResponseApplication(applicationRepository.save(application));
    }


    private void validateStatusTransition(ResumeStatus current, ResumeStatus next) {

        boolean valid = switch (current) {

            case PENDING -> next == ResumeStatus.REVIEWING
                    || next == ResumeStatus.REJECTED;

            case REVIEWING -> next == ResumeStatus.INTERVIEWING
                    || next == ResumeStatus.REJECTED;

            case INTERVIEWING -> next == ResumeStatus.OFFERED
                    || next == ResumeStatus.REJECTED;

            case OFFERED -> next == ResumeStatus.HIRED
                    || next == ResumeStatus.REJECTED;

            case HIRED, REJECTED -> false;
        };

        if (!valid) {
            throw new IllegalStateException(
                    String.format(
                            "Không thể chuyển trạng thái từ %s sang %s",
                            current,
                            next
                    )
            );
        }
    }

    @Transactional(readOnly = true)
    public ApplicationUserDetailResponse getApplicationDetailForUser(Long id) {

        Application application = applicationRepository
                .findDetailByIdAndUserId(id, securityUtils.getCurrentUserId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Không tìm thấy đơn ứng tuyển."
                ));

        return applicationMapper.toApplicationUserDetailResponse(application);
    }

    @Transactional(readOnly = true)
    public ApplicationHrDetailResponse getApplicationDetailForHR(Long id) {


        User currentUser = securityUtils.getCurrentUser();

        Application application = applicationRepository.findDetailById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn ứng tuyển."));

        Job job = application.getJob();

        if (job.getCompany() == null || !job.getCompany().getId().equals(currentUser.getCompany().getId())) {

            throw new ForbiddenException("Bạn không có quyền xem đơn ứng tuyển này.");
        }

        return applicationMapper.toApplicationHrDetailResponse(application);

    }

    @Transactional(readOnly = true)
    public ResponseDTO<List<AdminApplicationResponse>> getAllApplicationsForAdmin(int page, int size, ResumeStatus status) {

        size = Math.min(Math.max(size, 1), 50);
        page = Math.max(page, 1);

        Pageable pageable = PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "createdAt"));

        Specification<Application> spec = Specification.where(ApplicationSpecification.hasStatus(status));

        Page<Application> pagelist = applicationRepository.findAll(spec, pageable);
        int currentpage = pagelist.getNumber() + 1;
        int pagesize = pagelist.getSize();
        int totalpage = pagelist.getTotalPages();
        Long totalItem = pagelist.getTotalElements();

        MetaDTO metaDTO = new MetaDTO(currentpage, pagesize, totalpage, totalItem);

        List<Application> applications = pagelist.getContent();
        List<AdminApplicationResponse> list = applications.stream()
                .map(applicationMapper::mapAdminApplication)
                .toList();
        // 4. Trả về kết quả
        return new ResponseDTO<>(metaDTO, list);

    }
    @Transactional
    public void deleteByClient(Long applicationId) {

        Application application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException("Application không tồn tại"));

        if (!application.getUser().getId().equals(securityUtils.getCurrentUserId())) {
            throw new ForbiddenException("Bạn không có quyền xóa.");
        }

        if (application.getStatus() != ResumeStatus.PENDING) {
            throw new BadRequestException("Application đã được xử lý.");
        }

        Job job = application.getJob();

        if (job != null && job.getAppliedCount() > 0) {
            jobRepository.decreaseAppliedCount(job.getId());
        }

        applicationRepository.delete(application);
    }
    @Transactional
    public void deleteByAdmin(Long applicationId) {

        Application application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException("Application không tồn tại"));

        Job job = application.getJob();

        if (job != null && job.getAppliedCount() > 0) {
            jobRepository.decreaseAppliedCount(job.getId());
        }
        applicationRepository.delete(application);
    }
}
