package com.webjob.application.service;

import com.webjob.application.dto.Response.*;
import com.webjob.application.dto.Request.JobFilterAdminRequest;
import com.webjob.application.dto.Request.JobFilterClient;
import com.webjob.application.dto.Request.JobFilterHrRequest;
import com.webjob.application.enums.CompanyStatus;
import com.webjob.application.enums.JobStatus;
import com.webjob.application.exception.Customs.BadRequestException;
import com.webjob.application.exception.Customs.ForbiddenException;
import com.webjob.application.exception.Customs.UnauthorizedException;
import com.webjob.application.mapper.JobMapper;
import com.webjob.application.models.Entity.*;
import com.webjob.application.dto.Request.JobRequest;
import com.webjob.application.repository.CompanyRepository;
import com.webjob.application.repository.JobRepository;
import com.webjob.application.repository.SkillRepository;
import com.webjob.application.service.Specification.JobSpecification;
import com.webjob.application.utils.common.SecurityUtils;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class JobService {

    private final SkillRepository skillRepository;
    private final JobRepository jobRepository;

    private final ModelMapper modelMapper;
    private final CompanyRepository companyRepository;

    private final CompanyService companyService;

    private final PaymentService paymentService;
    private final SecurityUtils securityUtils;

    private final JobCategoryService jobCategoryService;
    private final JobMapper jobMapper;


    @Transactional
    @CacheEvict(value = "jobsCache", allEntries = true)
    public JobResponse createJob(JobRequest request) {
        checkNameJob(request.getName());
        User user = securityUtils.getCurrentUser();
        validateCompany(user);
        JobCategory category = jobCategoryService.findById(request.getJobCategoryId());
        Job job = modelMapper.map(request, Job.class);


        job.setCompany(user.getCompany());
        job.setJobCategory(category);
        job.setViewCount(0L);
        job.setAppliedCount(0);

        List<JobSkill> jobSkills = new ArrayList<>();
        if (request.getSkills() != null) {
            Map<Long, Skill> skillMaps = getSkillMapFromRequest(request);
            for (JobRequest.JobSkillRequest item : request.getSkills()) {
                Skill skill = skillMaps.get(item.getSkillId());
                if (skill == null) {
                    throw new BadRequestException("Skill not found with id: " + item.getSkillId());
                }
                JobSkill jobSkill = new JobSkill();
                jobSkill.setJob(job);
                jobSkill.setSkill(skill);
                jobSkill.setRequired(item.getRequired());
                jobSkill.setPriority(item.getPriority());
                jobSkill.setExperienceYear(item.getExperienceYear());
                jobSkill.setLevel(item.getLevel());

                jobSkills.add(jobSkill);
            }

        }


        job.setJobSkills(jobSkills);
        return jobMapper.toResponse(jobRepository.save(job));
    }

    private List<Skill> getValidSkills(List<Long> skills) {

        List<Skill> lists = skillRepository.findByIdIn(skills);
        if (lists.isEmpty()) {
            throw new BadRequestException("Không có kỹ năng nào hợp lệ.");
        }
        return lists;
    }


    @Transactional
    @CacheEvict(value = "jobsCache", allEntries = true)
    public JobResponse updateJob(Long id, JobRequest request) {
        Job job = getById(id);
        User user = securityUtils.getCurrentUser();
        validateCompany(user);

        // Giữ lại các field không được thay đổi
        Instant createdAt = job.getCreatedAt();
        String createdBy = job.getCreatedBy();
        Company company = job.getCompany();

        modelMapper.map(request, job);

        job.setCreatedAt(createdAt);
        job.setCreatedBy(createdBy);
        job.setCompany(company);

        // Update JobCategory

        if (request.getJobCategoryId() != null) {
            JobCategory category = jobCategoryService.findById(request.getJobCategoryId());
            job.setJobCategory(category);
        }
        if (request.getSkills() != null) {
            job.getJobSkills().clear();
            jobRepository.flush();   // Thực hiện DELETE ngay
            List<JobSkill> jobSkills = new ArrayList<>();
            Map<Long, Skill> skillMaps = getSkillMapFromRequest(request);
            for (JobRequest.JobSkillRequest item : request.getSkills()) {
                Skill skill = skillMaps.get(item.getSkillId());
                if (skill == null) {
                    throw new BadRequestException("Skill not found with id: " + item.getSkillId());
                }
                JobSkill jobSkill = new JobSkill();

                jobSkill.setJob(job);
                jobSkill.setSkill(skill);

                jobSkill.setRequired(item.getRequired());
                jobSkill.setPriority(item.getPriority());
                jobSkill.setExperienceYear(item.getExperienceYear());
                jobSkill.setLevel(item.getLevel());

                jobSkills.add(jobSkill);
            }
            job.getJobSkills().addAll(jobSkills);
        }
        Job edit = jobRepository.save(job);
        return jobMapper.toResponse(edit);
    }

    public boolean checkNameJob(String name) {
        boolean exist = jobRepository.existsByNameAndDeletedFalse(name);
        if (exist) {
            throw new IllegalArgumentException("Job name " + name + " da ton tai, vui long tao Job khac");
        }
        return false;
    }

    public Job getById(Long id) {
        Job job = jobRepository.findByIdAndDeletedFalse(id).
                orElseThrow(() -> new IllegalArgumentException("Job not found with ID: " + id));
        User user = securityUtils.getCurrentUser();
        validateCompany(user);
        return job;
    }


    public Page<Job> getAllPage(int page, int size, JobFilterAdminRequest request) {
        Specification<Job> specification =
                Specification.where(JobSpecification.keyword(request.getKeyword()));
        if (Boolean.TRUE.equals(request.getActiveOnly())) {
            specification = specification.and(
                    JobSpecification.activeOnly()
            );
        }

        specification = specification
                .and(JobSpecification.hasStatus(request.getStatus()))
                .and(JobSpecification.hasDeleted(request.getDeleted()))
                .and(JobSpecification.hasCompanies(request.getCompanyIds()))
                .and(JobSpecification.hasJobCategory(request.getJobCategoryId()))
                .and(JobSpecification.hasWorkingTypes(request.getWorkingTypes()))
                .and(JobSpecification.hasWorkModes(request.getWorkModes()))
                .and(JobSpecification.hasSalary(
                        request.getMinSalary(),
                        request.getMaxSalary()))
                .and(JobSpecification.hasExperience(
                        request.getExperience()))
                .and(JobSpecification.hasLevels(
                        request.getLevels()))
                .and(JobSpecification.createdBetween(request.getFrom(), request.getTo()))
                .and(JobSpecification.hasNegotiable(
                        request.getNegotiable()));
        Pageable pageable = PageRequest.of(page, size, jobMapper.toSort(request.getSort()));
        return jobRepository.findAll(specification, pageable);

    }


    public ResponseDTO<List<JobResponse>> searchJob(int page, int size, JobFilterClient request) {
        request = request == null ? new JobFilterClient() : request;

        if (page <= 0) {
            page = 1;
        }
        if (size <= 0) {
            size = 10;
        }
        Pageable pageable = PageRequest.of(page - 1, size, jobMapper.toSort(request.getSort()));
        Specification<Job> specification =
                Specification.where(JobSpecification.notDeleted())
                        .and(JobSpecification.activeOnly())
                        .and(JobSpecification.keyword(request.getKeyword()))
                        .and(JobSpecification.hasLocation(request.getLocation()))
                        .and(JobSpecification.hasJobCategory(request.getJobCategoryId()))
                        .and(JobSpecification.hasCompanies(request.getCompanyIds()))
                        .and(JobSpecification.companyActive())
                        .and(JobSpecification.hasSkills(request.getSkillIds()))
                        .and(JobSpecification.hasSalary(
                                request.getMinSalary(),
                                request.getMaxSalary()
                        ))
                        .and(JobSpecification.hasExperience(
                                request.getExperience()
                        ))
                        .and(JobSpecification.hasLevels(
                                request.getLevels()
                        ))
                        .and(JobSpecification.hasWorkingTypes(
                                request.getWorkingTypes()
                        ))
                        .and(JobSpecification.hasWorkModes(
                                request.getWorkModes()
                        ))
                        .and(JobSpecification.createdWithin(
                                request.getPostedDate()
                        ))
                        .and(JobSpecification.hasNegotiable(
                                request.getNegotiable()
                        ));
        Page<Job> result = jobRepository.findAll(specification, pageable);
        return convertToJobResponseDTO(result);

    }

    public Page<Job> getJobsCompany(int page, int size, JobFilterHrRequest request) {

        User user = securityUtils.getCurrentUser();
        validateCompany(user);
        Pageable pageable = PageRequest.of(page, size, jobMapper.toSort(request.getSort()));
        Specification<Job> specification =
                Specification.where(JobSpecification.hasCompany(user.getCompany().getId()));
        if (Boolean.TRUE.equals(request.getActiveOnly())) {
            specification = specification.and(
                    JobSpecification.activeOnly()
            );
        }

        specification=specification
                        .and(JobSpecification.keyword(request.getKeyword()))
                        .and(JobSpecification.hasStatus(request.getStatus()))
                        .and(JobSpecification.hasDeleted(request.getDeleted()))
                        .and(JobSpecification.hasJobCategory(request.getJobCategoryId()))
                        .and(JobSpecification.hasWorkingTypes(request.getWorkingTypes()))
                        .and(JobSpecification.hasWorkModes(request.getWorkModes()))
                        .and(JobSpecification.hasSalary(
                                request.getMinSalary(),
                                request.getMaxSalary()))
                        .and(JobSpecification.hasExperience(
                                request.getExperience()))
                        .and(JobSpecification.hasLevels(
                                request.getLevels()))
                        .and(JobSpecification.createdBetween(request.getFrom(), request.getTo()))
                        .and(JobSpecification.hasNegotiable(
                                request.getNegotiable()));

        return jobRepository.findAll(specification, pageable);
    }


    @Transactional
    @CacheEvict(value = "jobsCache", allEntries = true)
    public void deleteJob(Long id) {
        Job job = getById(id);

        User user = securityUtils.getCurrentUser();
        validateCompany(user);
        job.setDeleted(true);
        job.setDeletedAt(Instant.now());
        job.setStatus(JobStatus.CLOSED);
        job.setDeletedBy(user.getEmail());
        jobRepository.save(job);
    }

    @Transactional
    @CacheEvict(value = "jobsCache", allEntries = true)
    public void restoreJob(Long id) {
        User user = securityUtils.getCurrentUser();
        validateCompany(user);
        Job job = jobRepository.findByIdAndDeletedTrue(id)
                .orElseThrow(() -> new BadRequestException("Không tìm thấy Job nào đã xóa với ID: " + id));
        if (job.getCompany() == null) {
            throw new BadRequestException("Không thể khôi phục vì công ty không còn hoạt động.");
        }
        if (job.getJobCategory() == null) {
            throw new BadRequestException("Không thể khôi phục vì danh mục công việc không tồn tại.");
        }

        job.setDeleted(false);
        job.setDeletedAt(null);
        job.setDeletedBy(null);
        Instant now = Instant.now();

        if (job.getEndDate().isBefore(now)) {
            job.setStatus(JobStatus.EXPIRED);
        } else {
            job.setStatus(JobStatus.OPEN);
        }

        jobRepository.save(job);
    }


    public ResponseDTO<List<JobResponse>> getAllAdmin(int page, int size, JobFilterAdminRequest request) {
        if (request == null) {
            request = new JobFilterAdminRequest();

        }


        // Bỏ hoàn toàn try-catch, chỉ giữ lại logic kiểm tra số âm/bằng 0
        if (page <= 0) {
            page = 1;
        }
        if (size <= 0) {
            size = 10;
        }
        Page<Job> pagelist = getAllPage(page - 1, size, request);
        return convertToJobResponseDTO(pagelist);

    }

    public ResponseDTO<List<JobResponse>> getMyCompanyJobs(int page, int size, JobFilterHrRequest request) {
        request = request == null ? new JobFilterHrRequest() : request;
        // Bỏ hoàn toàn try-catch, chỉ giữ lại logic kiểm tra số âm/bằng 0
        if (page <= 0) {
            page = 1;
        }
        if (size <= 0) {
            size = 10;
        }
        Page<Job> pagelist = getJobsCompany(page - 1, size, request);
        return convertToJobResponseDTO(pagelist);

    }

    public ResponseDTO<List<JobResponse>> convertToJobResponseDTO(Page<Job> pagelist) {

        int currentpage = pagelist.getNumber() + 1;
        int pagesize = pagelist.getSize();
        int totalpage = pagelist.getTotalPages();
        Long totalItem = pagelist.getTotalElements();


        MetaDTO metaDTO = new MetaDTO(currentpage, pagesize, totalpage, totalItem);


        List<Job> jobsList = pagelist.getContent();
        List<JobResponse> list = jobsList.stream()
                .map(this.jobMapper::toResponse)
                .toList();

        // 4. Trả về kết quả
        return new ResponseDTO<>(metaDTO, list);
    }




    @Transactional
    public JobResponse detailJobId(Long id) {
        User user = securityUtils.getCurrentUser();
        validateCompany(user);
        int updated = jobRepository.increaseViewCount(id);
        if (updated == 0) {
            throw new BadRequestException("Job không tồn tại hoặc đã bị xóa");
        }
        Job job = getById(id);
        return jobMapper.toResponse(job);

    }


    public JobApplicantInfoResponse getJobApplicantInfo(Long jobId) {
        User userHR = securityUtils.getCurrentUser();
        validateCompany(userHR);
        return paymentService.getJobApplicantInfo(userHR.getId(), jobId);

    }

    private Map<Long, Skill> getSkillMapFromRequest(JobRequest request) {

        if (request.getSkills() == null || request.getSkills().isEmpty()) {
            return Map.of();
        }
        Set<Long> ids = request.getSkills()
                .stream()
                .map(JobRequest.JobSkillRequest::getSkillId)
                .collect(Collectors.toSet());

        List<Skill> skills = skillRepository.findAllById(ids);

        Map<Long, Skill> skillMap = skills.stream()
                .collect(Collectors.toMap(Skill::getId, Function.identity()));
        return skillMap;
    }
    private void validateCompany(User user) {

        String code = user.getRole().getCode();

        if (!code.startsWith("HR")) {
            return;
        }

        Company company = user.getCompany();

        if (company == null) {
            throw new ForbiddenException("No company is associated with this account.");
        }

        if (company.getDeleted()) {
            throw new ForbiddenException("Your company has been deleted.");
        }

        if (company.getStatus() != CompanyStatus.ACTIVE) {
            throw new ForbiddenException("Your company is inactive.");
        }
    }


}

