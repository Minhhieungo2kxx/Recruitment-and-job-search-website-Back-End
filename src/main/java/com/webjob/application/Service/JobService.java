package com.webjob.application.Service;

import com.webjob.application.Dto.Response.JobDTO;
import com.webjob.application.Model.Entity.Company;
import com.webjob.application.Model.Entity.Job;
import com.webjob.application.Model.Entity.Skill;
import com.webjob.application.Dto.Request.JobRequest;
import com.webjob.application.Dto.Request.Search.JobFiltersearch;
import com.webjob.application.Dto.Response.MetaDTO;
import com.webjob.application.Dto.Response.ResponseDTO;
import com.webjob.application.Repository.CompanyRepository;
import com.webjob.application.Repository.JobRepository;
import com.webjob.application.Repository.SkillRepository;
import com.webjob.application.Service.Specification.JobSpecification;
import jakarta.transaction.Transactional;
import org.modelmapper.ModelMapper;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class JobService {

    private final SkillRepository skillRepository;
    private final JobRepository jobRepository;

    private final ModelMapper modelMapper;
    private final CompanyRepository companyRepository;

    public JobService(SkillRepository skillRepository, JobRepository jobRepository, ModelMapper modelMapper, CompanyRepository companyRepository) {
        this.skillRepository = skillRepository;
        this.jobRepository = jobRepository;
        this.modelMapper = modelMapper;
        this.companyRepository = companyRepository;
    }


    @Transactional
    @CacheEvict(value = "jobsCache", allEntries = true)
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
    @CacheEvict(value = "jobsCache", allEntries = true)
    public Job updateJob(Long id, JobRequest request) {
        Job job = getById(id);
        Instant create = job.getCreatedAt();

        List<Skill> validSkills = getValidSkills(request.getSkills());

        if (validSkills.isEmpty()) {
            throw new IllegalArgumentException("Không có kỹ năng nào hợp lệ.");
        }

        modelMapper.map(request, job);
        job.setSkills(validSkills);
        job.setCreatedAt(create);
        if (request.getCompanyId() != null) {
            Company company = companyRepository.findById(request.getCompanyId())
                    .orElseThrow(() -> new IllegalArgumentException("Company không tồn tại"));
            job.setCompany(company);
        }

        return jobRepository.save(job);
    }

    public boolean checkNameJob(String name) {
        boolean exist = jobRepository.existsByName(name);
        if (exist) {
            throw new IllegalArgumentException("Job name " + name + " da ton tai, vui long tao Job khac");
        }
        return false;
    }

    public Job getById(Long id) {
        Job getjob = jobRepository.findById(id).
                orElseThrow(() -> new IllegalArgumentException("Job not found with ID: " + id));
        return getjob;
    }


    private List<Skill> getValidSkills(List<JobRequest.SkillIdDTO> skillDTOs) {
        List<Long> ids = skillDTOs.stream()
                .map(JobRequest.SkillIdDTO::getId)
                .collect(Collectors.toList());
        return skillRepository.findByIdIn(ids);
    }


    public Page<Job> getAllPage(int page, int size) {
        Sort.Direction direction = Sort.Direction.ASC;
        Sort sort = Sort.by(direction, "name");
        Pageable pageable = PageRequest.of(page, size, sort);
        return jobRepository.findAll(pageable);

    }


    public Page<Job> AllsearchJobs(int page, JobFiltersearch jobFilter) {
        Specification<Job> spec = Specification.where(JobSpecification.hasNameLike(jobFilter.getName())
                .and(JobSpecification.hasLocationLike(jobFilter.getLocation()))
                .and(JobSpecification.hasLevel(jobFilter.getLevel()))
                .and(JobSpecification.hasDescriptionLike(jobFilter.getDescription()))
                .and(JobSpecification.hasSalaryBetween(jobFilter.getMinSalary(), jobFilter.getMaxSalary()))
                .and(JobSpecification.hasDateRange(jobFilter.getStartDate(), jobFilter.getEndDate()))
                .and(JobSpecification.isActive(jobFilter.getActive()))
                .and(JobSpecification.hasSkills(jobFilter.getSkillIds()))
                .and(JobSpecification.hasCompetitionLevel(jobFilter.getCompetitionLevel()))
                .and(JobSpecification.hasJobCategory(jobFilter.getJobCategory()))
        );
        Sort.Direction direction = Sort.Direction.ASC;
        Sort sort = Sort.by(direction, "name");
        Pageable pageable = PageRequest.of(page, jobFilter.getSize(), sort);

        Page<Job> result = jobRepository.findAll(spec, pageable);
        System.out.println("✅ Saved to Redis cache for key: page_" + page + "_size_" + jobFilter.getSize());
        return result;


    }


    //    quan he n-n
    @Transactional
    // Khi thêm/sửa/xóa job => xóa toàn bộ cache liên quan
    @CacheEvict(value = "jobsCache", allEntries = true)
    public void deleteJob(Job job) {
        job.getSkills().clear();
        jobRepository.delete(job);
    }

    // Cache theo tham số tìm kiếm để không bị trùng key

    @Cacheable(value = "jobsCache", key = "'page_'+#jobFiltersearch.getPage() + '_size_'+#jobFiltersearch.getSize()")
    public ResponseDTO<?> getPaginated(JobFiltersearch jobFiltersearch, String type) {
        int page = 0;
        int size = 8;
        try {
            page = Integer.parseInt(jobFiltersearch.getPage());
            if (page <= 0)
                page = 1;
        } catch (NumberFormatException e) {
            // Nếu người dùng nhập sai, mặc định về trang đầu
            page = 1;
        }
        Page<Job> pagelist;
        if (type.equals("filter-job")) {
            pagelist = AllsearchJobs(page - 1, jobFiltersearch);
        } else {
            pagelist = getAllPage(page - 1, jobFiltersearch.getSize());
        }

        int currentpage = pagelist.getNumber() + 1;
        int pagesize = pagelist.getSize();
        int totalpage = pagelist.getTotalPages();
        Long totalItem = pagelist.getTotalElements();

        MetaDTO metaDTO = new MetaDTO(currentpage, pagesize, totalpage, totalItem);
        List<Job> jobsList = pagelist.getContent();
        List<JobDTO> list=jobsList.stream().map(job -> modelMapper.map(job,JobDTO.class)).toList();
        ResponseDTO<?> respond = new ResponseDTO<>(metaDTO,list);
        return respond;

    }


}
//
//|             Tình huống                             | Quan tâm bên nào?                     | Ghi chú                                                 |
//        | -------------------------------------- | ------------------------------------- | ------------------------------------------------------- |
//        | Xóa bản ghi "cha" (1)                  |  Rất nên kiểm tra bên "nhiều" (con) | Tránh lỗi khóa ngoại (`FK constraint`) hoặc mất dữ liệu |
//        | Xóa bản ghi "con" (nhiều)              |  Chủ yếu kiểm tra bên đó             | Có thể cần cập nhật quan hệ, hoặc làm mềm (soft delete) |
//        | Liên kết `@ManyToMany`                 | ️ Kiểm tra cả hai bên                | Xóa ở bảng trung gian (join table) là quan trọng        |
//        | Sử dụng `cascade = CascadeType.REMOVE` |  Quan trọng                          | Cho phép tự động xóa các bản ghi liên quan              |


//|              Tình huống                | Xử lý                      | Ghi vào DB                                    |
//        | ------------------------- | -------------------------- | --------------------------------------------- |
//        | `user.setCompany(null)`   | Gỡ liên kết `Many-to-One`  | Cập nhật FK trong `user` về NULL              |
//        | `job.getSkills().clear()` | Gỡ liên kết `Many-to-Many` | Xóa bản ghi trong bảng **join** (`job_skill`) |
