package com.webjob.application.service;

import com.webjob.application.dto.Response.*;
import com.webjob.application.models.Entity.Company;
import com.webjob.application.models.Entity.Job;
import com.webjob.application.models.Entity.Skill;
import com.webjob.application.dto.Request.JobRequest;
import com.webjob.application.dto.Request.Search.JobFiltersearch;
import com.webjob.application.models.Entity.User;
import com.webjob.application.repository.CompanyRepository;
import com.webjob.application.repository.JobRepository;
import com.webjob.application.repository.SkillRepository;
import com.webjob.application.service.Specification.JobSpecification;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class JobService {

    private final SkillRepository skillRepository;
    private final JobRepository jobRepository;

    private final ModelMapper modelMapper;
    private final CompanyRepository companyRepository;

    private final CompanyService companyService;

    private final UserService userService;
    private final PaymentService paymentService;


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
        System.out.println(" Saved to Redis cache for key: page_" + page + "_size_" + jobFilter.getSize());
        return result;


    }




    public void deleteJob(Job job) {
        job.getSkills().clear();
        jobRepository.delete(job);
    }



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
        List<JobDTO> list = jobsList.stream().map(job -> modelMapper.map(job, JobDTO.class)).toList();
        ResponseDTO<?> respond = new ResponseDTO<>(metaDTO, list);
        return respond;

    }

    @Transactional
    @CacheEvict(value = "jobsCache", allEntries = true)
    public ResponseEntity<?> create_newJob(JobRequest request) {
        checkNameJob(request.getName());
        Job created = createJob(request);
        JobResponse response = modelMapper.map(created, JobResponse.class);
        List<String> skillNames = created.getSkills().stream().map(Skill::getName)
                .collect(Collectors.toList());
        response.setSkills(skillNames);
        Company company = companyService.getbyID(request.getCompanyId())
                .orElseThrow(() -> new IllegalArgumentException("Company not found with ID: " + request.getCompanyId()));
        if (company != null) {
            response.setCompanyName(company.getName());
        }
        ApiResponse<?> apiResponse = new ApiResponse<>(HttpStatus.CREATED.value(), null,
                "Tạo job thành công",
                response);
        return new ResponseEntity<>(apiResponse, HttpStatus.CREATED);

    }
    @Transactional
    @CacheEvict(value = "jobsCache", allEntries = true)
    public ResponseEntity<?> edit_Job( Long id, JobRequest request) {
        Job update = updateJob(id,request);
        JobResponse response=modelMapper.map(update,JobResponse.class);
        List<String> skillNames = update.getSkills().stream().map(Skill::getName)
                .collect(Collectors.toList());
        response.setSkills(skillNames);
        Company company=companyService.getbyID(request.getCompanyId())
                .orElseThrow(() -> new IllegalArgumentException("Company not found with ID: " +request.getCompanyId()));
        if (company!=null){
            response.setCompanyName(company.getName());
        }
        ApiResponse<?> apiResponse=new ApiResponse<>(HttpStatus.OK.value(), null,
                "Update job thành công",
                response);
        return new ResponseEntity<>(apiResponse,HttpStatus.OK);

    }
    @Cacheable(value = "jobsCache", key = "'page_'+#jobFiltersearch.getPage() + '_size_'+#jobFiltersearch.getSize()")
    public ResponseEntity<?> GetallPageList(JobFiltersearch jobFiltersearch){
        ResponseDTO<?> respond=getPaginated(jobFiltersearch,"default");
        ApiResponse<?> response=new ApiResponse<>(HttpStatus.OK.value(), null
                ,"fetch all Jobs"
                ,getPaginated(jobFiltersearch,"default")
        );
        return ResponseEntity.ok(response);
    }
    public ResponseEntity<?> detailJob_Id( Long id) {
        Job job=getById(id);
        ApiResponse<?> apiResponse=new ApiResponse<>(HttpStatus.OK.value(), null,
                "Detail job thành công with "+id,
                job);
        return new ResponseEntity<>(apiResponse,HttpStatus.OK);
    }
    @Transactional
    @CacheEvict(value = "jobsCache", allEntries = true)
    public ResponseEntity<?> deleteJob_byId(Long id) {
        Job job=getById(id);
        deleteJob(job);
        ApiResponse<Object> response = new ApiResponse<>(HttpStatus.OK.value(),
                null,
                "Delete Job successful with "+id,
                null

        );
        return ResponseEntity.status(HttpStatus.NO_CONTENT).body(response);
    }
    @Cacheable(value = "jobsCache", key = "'page_'+#jobFiltersearch.getPage() + '_size_'+#jobFiltersearch.getSize()")
    public ResponseEntity<?> GetallSearch_Job( JobFiltersearch jobFiltersearch){
        ResponseDTO<?> respond=getPaginated(jobFiltersearch,"filter-job");
        ApiResponse<?> response=new ApiResponse<>(HttpStatus.OK.value(), null,
                "Filter all Jobs with condition Succesful",
                respond
        );
        return ResponseEntity.ok(response);

    }
    public ResponseEntity<?> getJob_ApplicantInfo(Long jobId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        User userHR = userService.getById(Long.valueOf(authentication.getName()));
        JobApplicantInfoResponse response = paymentService.getJobApplicantInfo(userHR.getId(), jobId);
        ApiResponse<?> apiResponse=new ApiResponse<>(
                HttpStatus.OK.value(),
                null,
                "Lấy thông tin ứng viên thành công",
                response
        );
        return ResponseEntity.ok(apiResponse);

    }


}

