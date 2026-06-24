package com.webjob.application.service;


import com.webjob.application.dto.Request.UpdateResumeHR;
import com.webjob.application.dto.Request.UpdateResumeUser;
import com.webjob.application.dto.Response.*;
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
import java.util.*;

@Service
@RequiredArgsConstructor
public class ResumService {

    private final JobService jobService;

    private final UserService userService;

    private final ResumeRepository resumeRepository;

    private final ModelMapper modelMapper;

    private final JobRepository jobRepository;
    private final FileService fileService;

    private final ApplicationEmailService applicationEmailService;

    private final TemporaryUploadRepository temporaryUploadRepository;






    public Resume saveResume(Resume resume,Authentication authentication) {
        // Lấy thông tin người dùng hiện tại từ context
        User user = userService.getById(Long.valueOf(authentication.getName()));
        Job job = jobService.getById(resume.getJob().getId());
        resume.setJob(job);
        resume.setUser(user);
        job.setAppliedCount(job.getAppliedCount() + 1);
        jobRepository.save(job);
        User hr = userService.getbyHR(job.getCompany());
//send Email
        applicationEmailService.sendJobApplicate(user, job, hr);
        return resumeRepository.save(resume);

    }


    public Resume updateStatusByHR(Long id, ResumeStatus newStatus,Authentication authentication) {
        Resume resume= getById(id);
        User userHR = userService.getById(Long.valueOf(authentication.getName()));
        Company company = userHR.getCompany();
        if (!resume.getJob().getCompany().getId().equals(company.getId())) {
            throw new AccessDeniedException("Không thuộc công ty của bạn");
        }
        ResumeStatus current = resume.getStatus();
        // State machine chuẩn
        if (current == ResumeStatus.PENDING && newStatus == ResumeStatus.REVIEWING) {
            resume.setStatus(newStatus);
        }
        else if (current == ResumeStatus.REVIEWING &&
                (newStatus == ResumeStatus.APPROVED ||
                        newStatus == ResumeStatus.REJECTED)) {
            resume.setStatus(newStatus);
        }
        else {
            throw new IllegalStateException("Chuyển trạng thái không hợp lệ");
        }

        return resumeRepository.save(resume);
    }

    public Resume updateResumeUser(Long resumeId, UpdateResumeUser dto,Authentication authentication) throws IOException {
        Resume resume =getById(resumeId);
        Long currentUserId =Long.valueOf(authentication.getName());
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
        //  Xóa file cũ nếu có
        if (resume.getPublicId() != null &&
                !resume.getPublicId().equals(dto.getPublicId())) {
            fileService.deleteFile(resume.getPublicId(),resume.getResourceType());
        }
        // Update resume
        resume.setUrl(dto.getUrl());
        resume.setPublicId(dto.getPublicId());
        resume.setResourceType(dto.getResourceType());

        //  Đánh dấu đã dùng
        temp.setUsed(true);
        temporaryUploadRepository.save(temp);
        return resumeRepository.save(resume);
    }

    public Resume getById(Long id) {
        Resume resume = resumeRepository.findById(id).
                orElseThrow(() -> new IllegalArgumentException("Resume not found with ID: " + id));
        return resume;
    }


    public void deleteResume(Resume resume) {

        try {
            // Xóa file trên Cloudinary
            fileService.deleteFile(resume.getPublicId().trim(),resume.getResourceType().trim());

            // Giảm appliedCount
            Job job = resume.getJob();
            if (job.getAppliedCount() > 0) {
                job.setAppliedCount(job.getAppliedCount() - 1);
                jobRepository.save(job);
            }

            // Xóa resume
            resumeRepository.delete(resume);

        } catch (IOException e) {
            throw new RuntimeException("Lỗi khi xóa file trên Cloudinary", e);
        }
    }

    public Page<Resume> getAllPage(int page, int size) {
        Sort.Direction direction = Sort.Direction.ASC;
        Sort sort = Sort.by(direction, "email");
        Pageable pageable = PageRequest.of(page, size, sort);
        return resumeRepository.findAll(pageable);

    }

    public Page<Resume> getAllResumbyuser(int page, int size,Authentication authentication) {
        User user = userService.getById(Long.valueOf(authentication.getName()));
        Pageable pageable = PageRequest.of(page, size);
        return resumeRepository.findAllByUser(user, pageable);
    }

    public Page<Resume> AllResumHRfromCompany(int page, int size,Authentication authentication) {
        User userHR = userService.getById(Long.valueOf(authentication.getName()));
        Company company = userHR.getCompany();
        Pageable pageable = PageRequest.of(page, size);
        return resumeRepository.findAllByJob_Company_Id(company.getId(), pageable);
    }

    // Phương thức chung để lấy resume với phân trang và ánh xạ
    public ResponseDTO<?> getPaginatedResumes(String pageparam, String type,Authentication authentication) {
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
            pagelist = getAllResumbyuser(page - 1, size,authentication);
        } else if (type.equals("HRfrom-Company")) {
            pagelist = AllResumHRfromCompany(page - 1, size,authentication);
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

    public ResponseDTO<?> getUserResumeHistory(String pageparam, ResumeStatus status,Authentication authentication) {
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
        Pageable pageable = PageRequest.of(page - 1, size,Sort.by(direction,"createdAt"));
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
    public ResponseEntity<?> create_Resume(Resume resume, Authentication authentication) {
        Resume resumeSave=saveResume(resume,authentication);
        ResumeResponse resumeResponse=modelMapper.map(resumeSave,ResumeResponse.class);
        if(resumeSave.getJob()!=null){
            resumeResponse.setCompanyName(resumeSave.getJob().getCompany().getName());
        }
        ApiResponse<?> apiResponse=new ApiResponse<>(HttpStatus.CREATED.value(), null,
                "Tạo Resume thành công",
                resumeResponse);
        return new ResponseEntity<>(apiResponse,HttpStatus.CREATED);

    }
    @Transactional
    public ResponseEntity<?> update_ResumeStatusHR(Long id
            ,UpdateResumeHR updateResumeDTO
            ,Authentication authentication) {
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
            ,UpdateResumeUser updateResumeDTO
            ,Authentication authentication)  {
        Resume edit = null;
        try {
            edit = updateResumeUser(id, updateResumeDTO, authentication);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
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
    public ResponseEntity<?> Getall_PageList(String pageparam,Authentication authentication) {
        ResponseDTO<?> respond = getPaginatedResumes(pageparam, "default", authentication);
        ApiResponse<?> response = new ApiResponse<>(HttpStatus.OK.value(), null,
                "Fetch all Resume Successful",
                respond
        );
        return ResponseEntity.ok(response);

    }
    public ResponseEntity<?> Getall_ResumebyUser(String pageparam,Authentication authentication) {
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






}
