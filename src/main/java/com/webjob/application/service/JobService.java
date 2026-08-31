package com.webjob.application.service;

import com.webjob.application.document.JobDocument;
import com.webjob.application.dto.Request.*;
import com.webjob.application.dto.Response.*;
import com.webjob.application.elasticsearch.job.JobElasticsearchSearchService;
import com.webjob.application.enums.CompanyStatus;
import com.webjob.application.enums.JobStatus;
import com.webjob.application.enums.OutboxCategory;
import com.webjob.application.enums.OutboxEventType;
import com.webjob.application.event.dto.JobCreatedEvent;
import com.webjob.application.exception.Customs.BadRequestException;
import com.webjob.application.exception.Customs.ForbiddenException;
import com.webjob.application.mapper.JobMapper;
import com.webjob.application.messaging.config.RabbitMQConfig;
import com.webjob.application.models.Entity.*;
import com.webjob.application.repository.CompanyRepository;
import com.webjob.application.repository.JobRepository;
import com.webjob.application.repository.SkillRepository;
import com.webjob.application.service.OutBox.OutboxService;
import com.webjob.application.service.Specification.JobSpecification;
import com.webjob.application.utils.common.SecurityUtils;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
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
    private final ApplicationEventPublisher publisher;

    private final JobElasticsearchSearchService elasticsearchSearchService;

    private final RedissonClient redissonClient;
    private final OutboxService outboxService;


    private static final long VIEW_TTL_HOURS = 24;


    @Transactional
    public JobResponse createJob(JobRequest request) {
        checkNameJob(request.getName());
        User user = securityUtils.getCurrentUser();
        validateCompany(user);
        validateSalary(request.getSalaryMin(), request.getSalaryMax());

        JobCategory category = jobCategoryService.findById(request.getJobCategoryId());
        Job job = modelMapper.map(request, Job.class);

        job.setCompany(user.getCompany());
        job.setJobCategory(category);
        job.setViewCount(0L);
        job.setAppliedCount(0);

        List<JobSkill> jobSkills = buildJobSkills(job, request);

        job.setJobSkills(jobSkills);

        Job saved = jobRepository.save(job);
        publishJobEvent(saved);
        outBoxJobEventIndex(saved);


        return jobMapper.toResponse(saved);
    }

    public List<JobSkill> buildJobSkills(Job job, JobRequest request) {
        List<JobSkill> jobSkills = new ArrayList<>();

        if (request.getSkills() == null || request.getSkills().isEmpty()) {
            return jobSkills;
        }

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

        return jobSkills;
    }


    public void validateSalary(Double salaryMin, Double salaryMax) {

        if (salaryMin != null
                && salaryMax != null
                && salaryMin > salaryMax) {

            throw new BadRequestException(
                    "Salary minimum cannot be greater than salary maximum.");
        }
    }


    @Transactional
    @CacheEvict(value = "jobsCache", allEntries = true)
    public JobResponse updateJob(Long id, JobRequest request) {
        Job job = getById(id);
        User user = securityUtils.getCurrentUser();
        validateCompany(user);
        validateSalary(request.getSalaryMin(), request.getSalaryMax());

        if (!job.getCompany().getId().equals(user.getCompany().getId())) {
            throw new ForbiddenException("You are not allowed to access this job.");
        }
        modelMapper.map(request, job);

        // Update JobCategory
        if (request.getJobCategoryId() != null) {
            JobCategory category = jobCategoryService.findById(request.getJobCategoryId());
            job.setJobCategory(category);
        }
        List<JobSkill> jobSkills = new ArrayList<>();
        if (request.getSkills() != null) {
            job.getJobSkills().clear();
            jobRepository.flush();   // Thực hiện DELETE ngay
            jobSkills = buildJobSkills(job, request);
        }
        job.setJobSkills(jobSkills);
        Job edit = jobRepository.save(job);

        updateJobIndexOutbox(edit);
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
        return job;
    }


    public Page<Job> getAllPage(int page, int size, JobFilterAdminRequest request) {
        Pageable fallbackPageable = PageRequest.of(page, size, jobMapper.toSort(request.getSort()));
        Specification<Job> specification = Specification.where(JobSpecification.keyword(request.getKeyword()));

        if (Boolean.TRUE.equals(request.getActiveOnly())) {
            specification = specification.and(JobSpecification.activeOnly());
        }
        specification = specification
                .and(JobSpecification.hasStatus(request.getStatus()))
                .and(JobSpecification.hasDeleted(request.getDeleted()))
                .and(JobSpecification.hasCompanies(request.getCompanyIds()))
                .and(JobSpecification.hasJobCategory(request.getJobCategoryId()))
                .and(JobSpecification.hasWorkingTypes(request.getWorkingTypes()))
                .and(JobSpecification.hasWorkModes(request.getWorkModes()))
                .and(JobSpecification.hasSalary(request.getMinSalary(), request.getMaxSalary()))
                .and(JobSpecification.hasExperience(request.getExperience()))
                .and(JobSpecification.hasLevels(request.getLevels()))
                .and(JobSpecification.createdBetween(request.getFrom(), request.getTo()))
                .and(JobSpecification.hasNegotiable(request.getNegotiable()));
        return jobRepository.findAll(specification, fallbackPageable);
//        }

    }

    private List<Job> reorderJobs(List<Long> ids, List<Job> jobs) {

        Map<Long, Job> jobMap = jobs.stream()
                .collect(Collectors.toMap(
                        Job::getId,
                        Function.identity()
                ));

        return ids.stream()
                .map(jobMap::get)
                .filter(Objects::nonNull)
                .toList();
    }


    public ResponseDTO<List<JobResponse>> searchJob(int page, int size, JobFilterClient request) {
        request = request == null ? new JobFilterClient() : request;

        if (page <= 0) {
            page = 1;
        }
        if (size <= 0) {
            size = 10;
        }
        Page<Job> result = searchJobClient(page - 1, size, request);
        return convertToJobResponseDTO(result);
    }

    public Page<Job> searchJobClient(int page, int size, JobFilterClient request) {
        Pageable pageable = PageRequest.of(page, size);
        ElasticsearchSearchResult result;
        try {
            result = elasticsearchSearchService.searchClient(page, size, request);
            long total = (result != null) ? result.getTotal() : 0;
            if (result == null || result.getIds() == null || result.getIds().isEmpty()) {
                return new PageImpl<>(Collections.emptyList(), pageable, total);
            }
            List<Job> jobs = jobRepository.findByIdIn(result.getIds());

            List<Job> orderedJobs = reorderJobs(result.getIds(), jobs);
            log.info("Search used Elasticsearch successful");
            return new PageImpl<>(orderedJobs, pageable, total);
        } catch (Exception e) {
            log.error(
                    "Elasticsearch search failed. Fallback to database. " + "page={}, size={}, keyword={}",
                    page, size, request.getKeyword(), e
            );
            Pageable fallbackPageable = PageRequest.of(page, size, jobMapper.toSort(request.getSort()));
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
            return jobRepository.findAll(specification, fallbackPageable);
        }

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
        specification = specification
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
    public void deleteJob(Long id) {
        Job job = getById(id);

        User user = securityUtils.getCurrentUser();
        validateCompany(user);
        if (!job.getCompany().getId().equals(user.getCompany().getId())) {
            throw new ForbiddenException("You are not allowed to access this job.");
        }
        job.setDeleted(true);
        job.setDeletedAt(Instant.now());
        job.setStatus(JobStatus.CLOSED);
        job.setDeletedBy(user.getEmail());
        jobRepository.save(job);
        JobIndexOutboxDeleted(job);


    }

    @Transactional
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
        if (!job.getCompany().getId().equals(user.getCompany().getId())) {
            throw new ForbiddenException("You are not allowed to access this job.");
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
        Job restored = jobRepository.save(job);

        restoreJobIndexOutbox(restored);

    }


    public ResponseDTO<List<JobResponse>> getAllAdmin(int page, int size, JobFilterAdminRequest request) {
        if (request == null) {
            request = new JobFilterAdminRequest();

        }
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

        Job job = getById(id);

        if (user != null) {
            boolean increased = increaseViewIfNeeded(
                    job.getId(),
                    user.getId()
            );
        }
        return jobMapper.toResponse(job);
    }


    private boolean increaseViewIfNeeded(Long jobId, Long userId) {
        String key = String.format("job:view:%d:user:%d", jobId, userId);
        RBucket<String> bucket = redissonClient.getBucket(key);

        boolean firstView = bucket.trySet(
                "1",
                VIEW_TTL_HOURS,
                TimeUnit.HOURS
        );
        if (!firstView) {
            return false;
        }

        jobRepository.increaseViewCount(jobId);
        JobViewCountIncrementedOutbox(jobId);

        return true;
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
        if (user == null) {
            return;
        }

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

    public void publishJobEvent(Job saved) {

        JobCreatedEvent event = JobCreatedEvent.builder()
                .companyId(saved.getCompany().getId())
                .companyName(saved.getCompany().getName())
                .local(saved.getLocation())
                .SalaryMin(saved.getSalaryMin())
                .SalaryMax(saved.getSalaryMax())
                .jobName(saved.getName())
                .jobId(saved.getId())
                .build();
        publisher.publishEvent(event);
        log.info("Publishing JobCreatedEvent for jobId: {}, jobName: {}, companyId: {}",
                event.getJobId(), event.getJobName(), event.getCompanyId());

    }

    public void outBoxJobEventIndex(Job saved) {
        JobDocument document = jobMapper.toDocument(saved);
        OutboxDTO dto = OutboxDTO.builder()
                .aggregateType("JOB")
                .aggregateId(saved.getId().toString())
                .category(OutboxCategory.JOB_INDEX)
                .eventType(OutboxEventType.JOB_INDEX_CREATED)
                .payload(document)
                .exchangeName(RabbitMQConfig.JOB_INDEX_EXCHANGE)
                .routingKey(RabbitMQConfig.JOB_INDEX_CREATED_ROUTING_KEY)
                .build();
        outboxService.save(dto);

    }

    public void updateJobIndexOutbox(Job saved) {
        JobDocument document = jobMapper.toDocument(saved);

        OutboxDTO dto = OutboxDTO.builder()
                .aggregateType("JOB")
                .aggregateId(saved.getId().toString())
                .category(OutboxCategory.JOB_INDEX)
                .eventType(OutboxEventType.JOB_INDEX_UPDATED)
                .payload(document)
                .exchangeName(RabbitMQConfig.JOB_INDEX_EXCHANGE)
                .routingKey(RabbitMQConfig.JOB_INDEX_UPDATED_ROUTING_KEY)
                .build();

        outboxService.save(dto);
    }

    public void JobIndexOutboxDeleted(Job job) {
        JobDocument document = JobDocument.builder()
                .id(job.getId())
                .deleted(job.isDeleted())
                .status(job.getStatus().name())
                .build();
        OutboxDTO dto = OutboxDTO.builder()
                .aggregateType("JOB")
                .aggregateId(job.getId().toString())
                .category(OutboxCategory.JOB_INDEX)
                .eventType(OutboxEventType.JOB_INDEX_DELETED)
                .payload(document)
                .exchangeName(RabbitMQConfig.JOB_INDEX_EXCHANGE)
                .routingKey(RabbitMQConfig.JOB_INDEX_DELETED_ROUTING_KEY)
                .build();
        outboxService.save(dto);
    }

    public void restoreJobIndexOutbox(Job job) {
        JobDocument document = JobDocument.builder()
                .id(job.getId())
                .status(job.getStatus().name())
                .deleted(job.isDeleted())
                .build();

        OutboxDTO dto = OutboxDTO.builder()
                .aggregateType("JOB")
                .aggregateId(job.getId().toString())
                .category(OutboxCategory.JOB_INDEX)
                .eventType(OutboxEventType.JOB_INDEX_RESTORED)
                .payload(document)
                .exchangeName(RabbitMQConfig.JOB_INDEX_EXCHANGE)
                .routingKey(RabbitMQConfig.JOB_INDEX_RESTORED_ROUTING_KEY)
                .build();
        outboxService.save(dto);
    }
    public void JobViewCountIncrementedOutbox(Long jobId) {
        JobDocument document = JobDocument.builder()
                .id(jobId)
                .build();

        OutboxDTO dto = OutboxDTO.builder()
                .aggregateType("JOB")
                .aggregateId(jobId.toString())
                .category(OutboxCategory.JOB_INDEX)
                .eventType(OutboxEventType.JOB_INDEX_VIEW_COUNT_INCREMENTED)
                .payload(document)
                .exchangeName(RabbitMQConfig.JOB_INDEX_EXCHANGE)
                .routingKey(RabbitMQConfig.JOB_INDEX_VIEW_COUNT_INCREMENTED_ROUTING_KEY)
                .build();
        outboxService.save(dto);
    }



}

