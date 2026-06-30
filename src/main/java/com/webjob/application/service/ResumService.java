package com.webjob.application.service;


import com.webjob.application.dto.Request.ResumeRequest;
import com.webjob.application.dto.Request.UpdateResumeHR;
import com.webjob.application.dto.Request.UpdateResumeUser;
import com.webjob.application.dto.Response.*;
import com.webjob.application.dto.record.ResumeFileDeletedEvent;
import com.webjob.application.exception.Customs.AlreadyAppliedException;
import com.webjob.application.exception.Customs.AppException;
import com.webjob.application.exception.Customs.JobExpiredException;
import com.webjob.application.exception.Customs.ResourceLockedException;
import com.webjob.application.messaging.dto.JobAppliedEvent;
import com.webjob.application.models.Entity.*;
import com.webjob.application.enums.ResumeStatus;
import com.webjob.application.repository.JobRepository;
import com.webjob.application.repository.ResumeRepository;
import com.webjob.application.repository.TemporaryUploadRepository;
import com.webjob.application.service.SendEmail.ApplicationEmailService;
import com.webjob.application.service.Specification.ResumeSpecification;
import com.webjob.application.service.UploadFileServer.FileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.transaction.annotation.Transactional;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class ResumService {

    private final JobService jobService;

    private final UserService userService;

    private final ResumeRepository resumeRepository;

    private final ModelMapper modelMapper;

    private final JobRepository jobRepository;


    private final ApplicationEmailService applicationEmailService;

    private final TemporaryUploadRepository temporaryUploadRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final RedissonClient redissonClient;


    public Resume saveResume(ResumeRequest request,
                             Authentication authentication) {
        User user = userService.getById(Long.valueOf(authentication.getName()));
        Job job = jobService.getById(request.getJobId());
        User hr = userService.getbyHR(job.getCompany());
        log.info(
                "User {} applied job {}",
                user.getId(),
                job.getId()
        );
        RLock lock = redissonClient.getLock(
                buildApplyLock(user.getId(), job.getId())
        );
        boolean acquired = false;
        try {

            acquired = lock.tryLock(5, TimeUnit.SECONDS
            );

            if (!acquired) {
                log.warn(
                        "Cannot acquire apply lock. userId={}, jobId={}",
                        user.getId(),
                        job.getId()
                );
                throw new ResourceLockedException(
                        "Bạn đang thực hiện thao tác ứng tuyển. Vui lòng thử lại."
                );

            }
            if (job.getEndDate().isBefore(Instant.now())) {
                throw new JobExpiredException("Công việc đã hết hạn.");
            }

            if (resumeRepository.existsByUserAndJob(user, job)) {
                throw new AlreadyAppliedException("Bạn đã ứng tuyển.");
            }
            TemporaryUpload upload = temporaryUploadRepository
                    .findByPublicId(request.getPublicId())
                    .orElseThrow(() -> new AppException("File không tồn tại"));

            if (!upload.getUserId().equals(user.getId())) {
                throw new AppException("CV không thuộc người dùng.");
            }
            if (upload.isUsed()) {
                throw new AppException("CV đã được sử dụng.");
            }

            Resume resume = new Resume();
            resume.setEmail(request.getEmail());
            resume.setJob(job);
            resume.setUser(user);
            resume.setStatus(ResumeStatus.PENDING);

            resume.setPublicId(upload.getPublicId());
            resume.setUrl(upload.getUrl());
            resume.setResourceType(upload.getResourceType());

            Resume saved = resumeRepository.save(resume);

            upload.setUsed(true);

            temporaryUploadRepository.save(upload);
            jobRepository.increaseAppliedCount(job.getId());

//            send email
            eventPublisher.publishEvent(
                    JobAppliedEvent.builder()
                            .email(user.getEmail())
                            .username(user.getFullName())
                            .usernameHR(hr.getFullName())
                            .companyName(job.getCompany().getName())
                            .companyLogo(job.getCompany().getLogo())
                            .jobName(job.getName())
                            .salary(BigDecimal.valueOf(job.getSalary()))
                            .location(job.getLocation())
                            .startDate(job.getStartDate())
                            .endDate(job.getEndDate())
                            .build()
            );
            log.info(
                    "Apply success. resumeId={}, userId={}, jobId={}",
                    saved.getId(),
                    user.getId(),
                    job.getId()
            );
            return saved;

        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new AppException("Không thể lấy lock. " + ex.getMessage());
        } catch (DataIntegrityViolationException ex) {
            log.info(
                    "Duplicate apply. userId={}, jobId={}",
                    user.getId(),
                    job.getId()
            );
            throw new AlreadyAppliedException(
                    "Bạn đã ứng tuyển công việc này."
            );

        } finally {

            if (acquired && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }

        }
    }


    public Resume updateStatusByHR(Long id, ResumeStatus newStatus, Authentication authentication) {
        Resume resume = getById(id);
        User userHR = userService.getById(Long.valueOf(authentication.getName()));
        Company company = userHR.getCompany();
        if (!resume.getJob().getCompany().getId().equals(company.getId())) {
            throw new AccessDeniedException("Không thuộc công ty của bạn");
        }
        ResumeStatus current = resume.getStatus();
        // State machine chuẩn
        if (current == ResumeStatus.PENDING && newStatus == ResumeStatus.REVIEWING) {
            resume.setStatus(newStatus);
        } else if (current == ResumeStatus.REVIEWING &&
                (newStatus == ResumeStatus.APPROVED ||
                        newStatus == ResumeStatus.REJECTED)) {
            resume.setStatus(newStatus);
        } else {
            throw new IllegalStateException("Chuyển trạng thái không hợp lệ");
        }

        return resumeRepository.save(resume);
    }

    public Resume updateResumeUser(Long resumeId, UpdateResumeUser dto, Authentication authentication) {
        Resume resume = getById(resumeId);
        Long currentUserId = Long.valueOf(authentication.getName());
        if (!resume.getUser().getId().equals(currentUserId)) {
            throw new AccessDeniedException("Không có quyền");
        }
        TemporaryUpload temp = temporaryUploadRepository
                .findByPublicId(dto.getPublicId())
                .orElseThrow(() -> new RuntimeException("File không tồn tại"));

        //  Validate file thuộc user
        if (!temp.getUserId().equals(currentUserId)) {
            throw new AccessDeniedException("File không thuộc quyền sở hữu");
        }

        //  Nếu file đã dùng rồi
        if (temp.isUsed()) {
            throw new IllegalStateException("File đã được sử dụng");
        }
        // Update resume
        resume.setUrl(dto.getUrl());
        resume.setPublicId(dto.getPublicId());
        resume.setResourceType(dto.getResourceType());
        //  Đánh dấu đã dùng
        temp.setUsed(true);
        temporaryUploadRepository.save(temp);
        Resume saved = resumeRepository.save(resume);
        //  Xóa file cũ nếu có
        String oldPublicId = resume.getPublicId();
        String oldResourceType = resume.getResourceType();
//        delete file cu
        if (oldPublicId != null &&
                !oldPublicId.equals(dto.getPublicId())) {
            eventPublisher.publishEvent(
                    new ResumeFileDeletedEvent(
                            oldPublicId,
                            oldResourceType
                    )
            );
        }

        return saved;

    }

    public Resume getById(Long id) {
        Resume resume = resumeRepository.findById(id).
                orElseThrow(() -> new IllegalArgumentException("Resume not found with ID: " + id));
        return resume;
    }


    public void deleteResume(Resume resume) {
        String oldPublicId = resume.getPublicId();
        String oldResourceType = resume.getResourceType();

        // Giảm appliedCount
        Job job = resume.getJob();
        if (job != null && job.getAppliedCount() > 0) {

            jobRepository.decreaseAppliedCount(job.getId());
        }


        // Xóa resume
        resumeRepository.delete(resume);

        TemporaryUpload temp = temporaryUploadRepository
                .findByPublicId(oldPublicId)
                .orElseThrow(() -> new RuntimeException("File không tồn tại"));
        temporaryUploadRepository.delete(temp);

        // Xóa file trên Cloudinary
        eventPublisher.publishEvent(
                new ResumeFileDeletedEvent(
                        oldPublicId,
                        oldResourceType
                )
        );

    }

    public Page<Resume> getAllPage(int page, int size) {
        Sort.Direction direction = Sort.Direction.ASC;
        Sort sort = Sort.by(direction, "email");
        Pageable pageable = PageRequest.of(page, size, sort);
        return resumeRepository.findAll(pageable);

    }

    public Page<Resume> getAllResumbyuser(int page, int size, Authentication authentication) {
        User user = userService.getById(Long.valueOf(authentication.getName()));
        Pageable pageable = PageRequest.of(page, size);
        return resumeRepository.findAllByUser(user, pageable);
    }

    public Page<Resume> AllResumHRfromCompany(int page, int size, Authentication authentication) {
        User userHR = userService.getById(Long.valueOf(authentication.getName()));
        Company company = userHR.getCompany();
        Pageable pageable = PageRequest.of(page, size);
        return resumeRepository.findAllByJob_Company_Id(company.getId(), pageable);
    }

    // Phương thức chung để lấy resume với phân trang và ánh xạ
    public ResponseDTO<?> getPaginatedResumes(String pageparam, String type, Authentication authentication) {
        int page = 1;
        int size = 8;
        try {
            page = Integer.parseInt(pageparam);
            if (page <= 0) page = 1;
        } catch (NumberFormatException e) {
            page = 1; // mặc định về trang đầu tiên nếu input không hợp lệ
        }

        Page<Resume> pagelist;

        // Nếu là yêu cầu của user, filter thêm theo userId
        if (type.equals("by-user")) {
            pagelist = getAllResumbyuser(page - 1, size, authentication);
        } else if (type.equals("HRfrom-Company")) {
            pagelist = AllResumHRfromCompany(page - 1, size, authentication);
        } else {
            pagelist = getAllPage(page - 1, size);
        }
        // Tạo MetaDTO
        int currentPage = pagelist.getNumber() + 1;
        int pageSize = pagelist.getSize();
        int totalPages = pagelist.getTotalPages();
        Long totalItems = pagelist.getTotalElements();

        MetaDTO metaDTO = new MetaDTO(currentPage, pageSize, totalPages, totalItems);
        // Ánh xạ danh sách Resume sang ResumeResponse
        List<ResumeResponse> responseList = new ArrayList<>();
        for (Resume resume : pagelist.getContent()) {
            ResumeResponse resumeResponse = modelMapper.map(resume, ResumeResponse.class);
            if (resume.getJob() != null) {
                resumeResponse.setCompanyName(resume.getJob().getCompany().getName());
            }
            responseList.add(resumeResponse);
        }
        // Tạo ResponseDTO
        ResponseDTO<?> responseDTO = new ResponseDTO<>(metaDTO, responseList);
        return responseDTO;
    }

    public ResponseDTO<?> getUserResumeHistory(String pageparam, ResumeStatus status, Authentication authentication) {
        int page = 1;
        int size = 8;
        try {
            page = Integer.parseInt(pageparam);
            if (page <= 0) page = 1;
        } catch (NumberFormatException e) {
            page = 1; // mặc định về trang đầu tiên nếu input không hợp lệ
        }
        User user = userService.getById(Long.valueOf(authentication.getName()));
        Sort.Direction direction = Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(page - 1, size, Sort.by(direction, "createdAt"));
        Specification<Resume> spec = Specification
                .where(ResumeSpecification.hasUserId(user.getId()))
                .and(ResumeSpecification.hasStatus(status));
        Page<Resume> resumePage = resumeRepository.findAll(spec, pageable);
        // Tạo MetaDTO

        MetaDTO metaData = MetaDTO.builder().current(resumePage.getNumber() + 1)
                .pageSize(resumePage.getSize())
                .pages(resumePage.getTotalPages())
                .total(resumePage.getTotalElements())
                .build();
        List<ResumeHistoryResponse> responseList = resumePage.getContent().stream()
                .filter(resume -> resume.getJob() != null && resume.getJob().getCompany() != null) // tránh null
                .map(resume -> new ResumeHistoryResponse(
                        resume.getJob().getCompany().getName(),
                        resume.getJob().getName(),
                        resume.getJob().getCompany().getLogo(),
                        resume.getCreatedAt(),
                        resume.getUrl(),
                        BigDecimal.valueOf(resume.getJob().getSalary()).toPlainString(),
                        resume.getStatus()
                ))
                .toList();
        ResponseDTO<?> responseDTO = ResponseDTO.builder().meta(metaData).result(responseList).build();
        return responseDTO;

    }

    @Transactional
    public ResponseEntity<?> create_Resume(ResumeRequest resume, Authentication authentication) {
        Resume resumeSave = saveResume(resume, authentication);
        ResumeResponse resumeResponse = modelMapper.map(resumeSave, ResumeResponse.class);
        if (resumeSave.getJob() != null) {
            resumeResponse.setCompanyName(resumeSave.getJob().getCompany().getName());
        }
        ApiResponse<?> apiResponse = new ApiResponse<>(HttpStatus.CREATED.value(), null,
                "Tạo Resume thành công",
                resumeResponse);
        return new ResponseEntity<>(apiResponse, HttpStatus.CREATED);

    }

    @Transactional
    public ResponseEntity<?> update_ResumeStatusHR(Long id
            , UpdateResumeHR updateResumeDTO
            , Authentication authentication) {
        Resume edit = updateStatusByHR(id, updateResumeDTO.getStatus(), authentication);
        ResumeResponse resumeResponse = modelMapper.map(edit, ResumeResponse.class);
        if (edit.getJob() != null) {
            resumeResponse.setCompanyName(edit.getJob().getCompany().getName());
        }
        ApiResponse<?> apiResponse = new ApiResponse<>(HttpStatus.OK.value(), null,
                "Cập nhật trạng thái Resume ứng viên thành công",
                resumeResponse);
        return ResponseEntity.ok(apiResponse);

    }

    @Transactional
    public ResponseEntity<?> update_ResumeUser(Long id
            , UpdateResumeUser updateResumeDTO
            , Authentication authentication) {
        Resume edit = null;
        edit = updateResumeUser(id, updateResumeDTO, authentication);

        ResumeResponse resumeResponse = modelMapper.map(edit, ResumeResponse.class);
        if (edit.getJob() != null) {
            resumeResponse.setCompanyName(edit.getJob().getCompany().getName());
        }
        ApiResponse<?> apiResponse = new ApiResponse<>(HttpStatus.OK.value(), null,
                "Cập nhật Resume thành công",
                resumeResponse);
        return ResponseEntity.ok(apiResponse);

    }

    @Transactional
    public ResponseEntity<?> delete_ResumebyId(Long id) {
        Resume resume = getById(id);
        deleteResume(resume);
        ApiResponse<?> response = new ApiResponse<>(HttpStatus.OK.value(),
                null,
                "Delete Resume successful with " + id,
                null

        );
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    public ResponseEntity<?> detail_ResumebyId(@PathVariable Long id) {
        Resume resume = getById(id);
        ResumeResponse resumeResponse = modelMapper.map(resume, ResumeResponse.class);
        if (resume.getJob() != null) {
            resumeResponse.setCompanyName(resume.getJob().getCompany().getName());
        }
        ApiResponse<?> response = new ApiResponse<>(HttpStatus.OK.value(),
                null,
                "Detail Resume successful with " + id,
                resumeResponse

        );
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    public ResponseEntity<?> Getall_PageList(String pageparam, Authentication authentication) {
        ResponseDTO<?> respond = getPaginatedResumes(pageparam, "default", authentication);
        ApiResponse<?> response = new ApiResponse<>(HttpStatus.OK.value(), null,
                "Fetch all Resume Successful",
                respond
        );
        return ResponseEntity.ok(response);

    }

    public ResponseEntity<?> Getall_ResumebyUser(String pageparam, Authentication authentication) {
        ResponseDTO<?> respond = getPaginatedResumes(pageparam, "by-user", authentication);
        ApiResponse<?> response = new ApiResponse<>(HttpStatus.OK.value(), null,
                "Fetch all Resumes by User Successful",
                respond
        );
        return ResponseEntity.ok(response);
    }

    public ResponseEntity<?> Getall_ResumeHRcompany(String pageparam, Authentication authentication) {

        ResponseDTO<?> respond = getPaginatedResumes(pageparam, "HRfrom-Company", authentication);
        ApiResponse<?> response = new ApiResponse<>(HttpStatus.OK.value(), null,
                "Fetch all Resumes from Company Successful",
                respond
        );
        return ResponseEntity.ok(response);
    }

    public ResponseEntity<?> get_ResumeHistoryApplied(String page, ResumeStatus status, Authentication authentication) {
        ResponseDTO<?> respond = getUserResumeHistory(page, status, authentication);
        ApiResponse<?> response = ApiResponse.builder().statusCode(HttpStatus.OK.value())
                .error(null)
                .message("Fetch all History Applied Resumes Apply Successful")
                .data(respond)
                .build();
        return ResponseEntity.ok(response);
    }

    private String buildApplyLock(Long userId, Long jobId) {
        return "lock:apply:" + userId + ":" + jobId;
    }

//    feat(resume): improve job application workflow with concurrency control and async email
//
//- prevent duplicate job applications using Redisson distributed locking
//- keep database unique constraint as the final consistency safeguard
//- replace read-modify-write appliedCount updates with atomic SQL increment/decrement
//- validate job availability and uploaded resume ownership before applying
//- publish JobAppliedEvent after transaction commit
//- process application emails asynchronously via RabbitMQ
//- add Dead Letter Queue (DLQ) consumer for failed email notifications
//- improve logging for application, event publishing, and email processing
}
