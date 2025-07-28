package com.webjob.application.Services;

import com.webjob.application.Models.Company;
import com.webjob.application.Models.Job;
import com.webjob.application.Models.Request.JobRequest;
import com.webjob.application.Models.Skill;
import com.webjob.application.Models.User;
import com.webjob.application.Repository.CompanyRepository;
import com.webjob.application.Repository.JobRepository;
import com.webjob.application.Repository.SkillRepository;
import jakarta.transaction.Transactional;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class JobService {
    @Autowired
    private SkillRepository skillRepository;
    @Autowired
    private JobRepository jobRepository;
    @Autowired
    private ModelMapper modelMapper;
    @Autowired
    private CompanyRepository companyRepository;

    @Transactional
    public Job createJob(JobRequest request) {
        List<Skill> validSkills = getValidSkills(request.getSkills());

        if (validSkills.isEmpty()) {
            throw new IllegalArgumentException("Không có kỹ năng nào hợp lệ.");
        }


        Job job = modelMapper.map(request, Job.class);
        job.setSkills(validSkills);

        if (request.getCompanyId() != null) {
            Company company = companyRepository.findById(request.getCompanyId())
                    .orElseThrow(() -> new IllegalArgumentException("Company không tồn tại"));
            job.setCompany(company);
        }

        return jobRepository.save(job);
    }
    @Transactional
    public Job updateJob(Long id,JobRequest request) {
        Job job =getById(id);
        Instant create=job.getCreatedAt();

        List<Skill> validSkills = getValidSkills(request.getSkills());

        if (validSkills.isEmpty()) {
            throw new IllegalArgumentException("Không có kỹ năng nào hợp lệ.");
        }

        modelMapper.map(request,job);
        job.setSkills(validSkills);
        job.setCreatedAt(create);
        if (request.getCompanyId() != null) {
            Company company = companyRepository.findById(request.getCompanyId())
                    .orElseThrow(() -> new IllegalArgumentException("Company không tồn tại"));
            job.setCompany(company);
        }

        return jobRepository.save(job);
    }
    public boolean checkNameJob(String name){
        boolean exist=jobRepository.existsByName(name);
        if (exist){
            throw new IllegalArgumentException("Job name "+name+" da ton tai, vui long tao Job khac");
        }
        return false;
    }
    public Job getById(Long id){
        Job getjob=jobRepository.findById(id).
                orElseThrow(() -> new IllegalArgumentException("Job not found with ID: " +id));
        return getjob;
    }


    private List<Skill> getValidSkills(List<JobRequest.SkillIdDTO> skillDTOs) {
        List<Long> ids = skillDTOs.stream()
                .map(JobRequest.SkillIdDTO::getId)
                .collect(Collectors.toList());
        return skillRepository.findByIdIn(ids);
    }
    public Page<Job> getAllPage(int page, int size){
        Sort.Direction direction=Sort.Direction.ASC;
        Sort sort=Sort.by(direction,"name");
        Pageable pageable= PageRequest.of(page,size,sort);
        return jobRepository.findAll(pageable);

    }
//    quan he n-n
    @Transactional
    public void deleteJob(Job job){
        job.getSkills().clear();
        jobRepository.delete(job);
    }

}
//
//|             Tình huống                             | Quan tâm bên nào?                     | Ghi chú                                                 |
//        | -------------------------------------- | ------------------------------------- | ------------------------------------------------------- |
//        | Xóa bản ghi "cha" (1)                  | ⚠️ Rất nên kiểm tra bên "nhiều" (con) | Tránh lỗi khóa ngoại (`FK constraint`) hoặc mất dữ liệu |
//        | Xóa bản ghi "con" (nhiều)              | ✅ Chủ yếu kiểm tra bên đó             | Có thể cần cập nhật quan hệ, hoặc làm mềm (soft delete) |
//        | Liên kết `@ManyToMany`                 | ⚠️ Kiểm tra cả hai bên                | Xóa ở bảng trung gian (join table) là quan trọng        |
//        | Sử dụng `cascade = CascadeType.REMOVE` | ✅ Quan trọng                          | Cho phép tự động xóa các bản ghi liên quan              |


//|              Tình huống                | Xử lý                      | Ghi vào DB                                    |
//        | ------------------------- | -------------------------- | --------------------------------------------- |
//        | `user.setCompany(null)`   | Gỡ liên kết `Many-to-One`  | Cập nhật FK trong `user` về NULL              |
//        | `job.getSkills().clear()` | Gỡ liên kết `Many-to-Many` | Xóa bản ghi trong bảng **join** (`job_skill`) |
