package com.webjob.application.Service;


import com.webjob.application.Model.Entity.*;
import com.webjob.application.Model.Enums.ResumeStatus;
import com.webjob.application.Dto.Request.UpdateResumeDTO;
import com.webjob.application.Dto.Response.MetaDTO;
import com.webjob.application.Dto.Response.ResponseDTO;
import com.webjob.application.Dto.Response.ResumeHistoryResponse;
import com.webjob.application.Dto.Response.ResumeResponse;
import com.webjob.application.Repository.JobRepository;
import com.webjob.application.Repository.ResumeRepository;
import com.webjob.application.Service.SendEmail.ApplicationEmailService;
import com.webjob.application.Service.Specification.ResumeSpecification;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.transaction.annotation.Transactional;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;

@Service
public class ResumService {

    private final JobService jobService;

    private final UserService userService;

    private final ResumeRepository resumeRepository;

    private final ModelMapper modelMapper;

    private final JobRepository jobRepository;


    private final ApplicationEmailService applicationEmailService;

    public ResumService(JobService jobService, UserService userService, ResumeRepository resumeRepository, ModelMapper modelMapper, JobRepository jobRepository, ApplicationEmailService applicationEmailService) {
        this.jobService = jobService;
        this.userService = userService;
        this.resumeRepository = resumeRepository;
        this.modelMapper = modelMapper;
        this.jobRepository = jobRepository;
        this.applicationEmailService = applicationEmailService;
    }

    @Transactional
    public Resume saveResume(Resume resume) {
        // Lấy thông tin người dùng hiện tại từ context
        String userEmail = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userService.getbyEmail(userEmail);
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

    @Transactional
    public Resume editResume(Long id, UpdateResumeDTO updateResumeDTO) {
        Resume update = getById(id);
        Instant instant = update.getCreatedAt();
        modelMapper.map(updateResumeDTO, update);
        update.setCreatedAt(instant);
        return resumeRepository.save(update);
    }

    public Resume getById(Long id) {
        Resume resume = resumeRepository.findById(id).
                orElseThrow(() -> new IllegalArgumentException("Resume not found with ID: " + id));
        return resume;
    }

    @Transactional
    public void deleteResume(Resume resume) {
        Job job = resume.getJob();
        resumeRepository.delete(resume);
        // Giảm appliedCount nếu > 0
        if (job.getAppliedCount() > 0) {
            job.setAppliedCount(job.getAppliedCount() - 1);
            jobRepository.save(job);
        }
    }

    public Page<Resume> getAllPage(int page, int size) {
        Sort.Direction direction = Sort.Direction.ASC;
        Sort sort = Sort.by(direction, "email");
        Pageable pageable = PageRequest.of(page, size, sort);
        return resumeRepository.findAll(pageable);

    }

    public Page<Resume> getAllResumbyuser(int page, int size) {
//        Sort.Direction direction=Sort.Direction.ASC;
//        Sort sort=Sort.by(direction,"email");
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();
        User user = userService.getbyEmail(email);
        Pageable pageable = PageRequest.of(page, size);
        return resumeRepository.findAllByUser(user, pageable);
    }

    public Page<Resume> AllResumHRfromCompany(int page, int size) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();
        User userHR = userService.getbyEmail(email);
        Company company = userHR.getCompany();
        Pageable pageable = PageRequest.of(page, size);
        return resumeRepository.findAllByJob_Company_Id(company.getId(), pageable);
    }

    // Phương thức chung để lấy resume với phân trang và ánh xạ
    public ResponseDTO<?> getPaginatedResumes(String pageparam, String type) {
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
            pagelist = getAllResumbyuser(page - 1, size);
        } else if (type.equals("HRfrom-Company")) {
            pagelist = AllResumHRfromCompany(page - 1, size);
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

    public ResponseDTO<?> getUserResumeHistory(String pageparam, ResumeStatus status) {
        int page = 1;
        int size = 8;
        try {
            page = Integer.parseInt(pageparam);
            if (page <= 0) page = 1;
        } catch (NumberFormatException e) {
            page = 1; // mặc định về trang đầu tiên nếu input không hợp lệ
        }
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();
        User user = userService.getbyEmail(email);
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



//Spring sử dụng proxy (uỷ quyền) để xử lý các annotation như @Async, @Transactional, @Cacheable, v.v.
// Các proxy này chỉ có tác dụng khi method được gọi từ bên ngoài class, thông qua Spring Container (ApplicationContext).
//Proxy sẽ xử lý:
//
//Bắt đầu luồng mới (@Async)
//
//Mở transaction (@Transactional)
//
//Bắt/ghi cache (@Cacheable)
//
//v.v...
//
// Nhưng điều này chỉ xảy ra nếu method được gọi qua proxy, tức từ một class khác (hoặc một bean khác).


}
