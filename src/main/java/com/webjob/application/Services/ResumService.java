package com.webjob.application.Services;


import com.webjob.application.Models.Entity.Company;
import com.webjob.application.Models.Entity.Job;
import com.webjob.application.Models.Request.UpdateResumeDTO;
import com.webjob.application.Models.Response.MetaDTO;
import com.webjob.application.Models.Response.ResponseDTO;
import com.webjob.application.Models.Response.ResumeResponse;
import com.webjob.application.Models.Entity.Resume;
import com.webjob.application.Models.Entity.User;
import com.webjob.application.Repository.JobRepository;
import com.webjob.application.Repository.ResumeRepository;
import jakarta.transaction.Transactional;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
public class ResumService {
    @Autowired
    private JobService jobService;
    @Autowired
    private UserService userService;
    @Autowired
    private ResumeRepository resumeRepository;
    @Autowired
    private ModelMapper modelMapper;
    @Autowired
    private JobRepository jobRepository;

    @Transactional
    public Resume saveResume(Resume resume){
        User user=userService.getbyID(resume.getUser().getId())
                .orElseThrow(() -> new IllegalArgumentException("User not found with ID: " +resume.getUser().getId()));
        Job job=jobService.getById(resume.getJob().getId());
        resume.setJob(job);
        resume.setUser(user);
        job.setAppliedCount(job.getAppliedCount()+1);
        jobRepository.save(job);
        return resumeRepository.save(resume);

    }
    @Transactional
    public Resume editResume(Long id,UpdateResumeDTO updateResumeDTO){
        Resume update=getById(id);
        Instant instant=update.getCreatedAt();
        modelMapper.map(updateResumeDTO,update);
        update.setCreatedAt(instant);
        return resumeRepository.save(update);
    }

    public Resume getById(Long id){
        Resume resume=resumeRepository.findById(id).
                orElseThrow(() -> new IllegalArgumentException("Resume not found with ID: " +id));
        return resume;
    }
    @Transactional
    public void deleteResume(Resume resume){
        Job job=resume.getJob();
        resumeRepository.delete(resume);
        // Giảm appliedCount nếu > 0
        if (job.getAppliedCount() > 0) {
            job.setAppliedCount(job.getAppliedCount() - 1);
            jobRepository.save(job);
        }
    }
    public Page<Resume> getAllPage(int page, int size){
        Sort.Direction direction=Sort.Direction.ASC;
        Sort sort=Sort.by(direction,"email");
        Pageable pageable= PageRequest.of(page,size,sort);
        return resumeRepository.findAll(pageable);

    }
    public Page<Resume> getAllResumbyuser(int page, int size){
//        Sort.Direction direction=Sort.Direction.ASC;
//        Sort sort=Sort.by(direction,"email");
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();
        User user = userService.getbyEmail(email);
        Pageable pageable= PageRequest.of(page,size);
        return resumeRepository.findAllByUser(user,pageable);
    }
    public Page<Resume> AllResumHRfromCompany(int page, int size){
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();
        User userHR = userService.getbyEmail(email);
        Company company =userHR.getCompany();
        Pageable pageable= PageRequest.of(page,size);
        return resumeRepository.findAllByJob_Company_Id(company.getId(),pageable);
    }

    // Phương thức chung để lấy resume với phân trang và ánh xạ
    public ResponseDTO<?> getPaginatedResumes(String pageparam,String type) {
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
            pagelist=getAllResumbyuser(page-1,size);
        }
        else if (type.equals("HRfrom-Company")){
            pagelist=AllResumHRfromCompany(page-1,size);
        }

        else {
            pagelist =getAllPage(page-1,size);
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
        ResponseDTO<?> responseDTO =new ResponseDTO<>(metaDTO,responseList);
        return responseDTO;
    }





}
