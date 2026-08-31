package com.webjob.application.service.ChatBox;

import com.webjob.application.dto.Request.JobSearchRequestAI;
import com.webjob.application.dto.Request.SearchCompanyAI;
import com.webjob.application.dto.Response.*;
import com.webjob.application.dto.record.AlertMatchResult;
import com.webjob.application.dto.record.SkillMatchResult;
import com.webjob.application.elasticsearch.company.CompanyElasticsearchSearchService;
import com.webjob.application.elasticsearch.job.JobElasticsearchSearchService;
import com.webjob.application.enums.JobLevel;
import com.webjob.application.enums.ResumeStatus;
import com.webjob.application.enums.WorkMode;
import com.webjob.application.enums.WorkingType;
import com.webjob.application.exception.Customs.AppException;
import com.webjob.application.exception.Customs.BadRequestException;
import com.webjob.application.exception.Customs.ResourceLockedException;
import com.webjob.application.exception.Customs.ResourceNotFoundException;
import com.webjob.application.mapper.ApplicationMapper;
import com.webjob.application.mapper.CompanyMapper;
import com.webjob.application.mapper.JobMapper;
import com.webjob.application.mapper.SavedJobMapper;
import com.webjob.application.models.Entity.*;
import com.webjob.application.repository.*;
import com.webjob.application.service.Specification.CompanySpecification;
import com.webjob.application.service.Specification.JobSpecification;
import com.webjob.application.utils.common.SecurityUtils;
import com.webjob.application.utils.common.UtilFormat;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.webjob.application.utils.common.UtilFormat.parseEnumSafe;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChatToolService {
    private final JobRepository jobRepository;

    private final JobMapper jobMapper;

    private final RedissonClient redissonClient;

    private final CompanyRepository companyRepository;

    private final CompanyMapper companyMapper;
    private final ApplicationRepository applicationRepository;
    private final ApplicationMapper applicationMapper;
    private final SecurityUtils securityUtils;
    private final SavedJobRepository savedJobRepository;
    private final UserRepository userRepository;

    private final SubscriberRepository subscriberRepository;
    private final JobAlertRepository jobAlertRepository;

    private final SavedJobMapper savedJobMapper;

    private final JobElasticsearchSearchService elasticsearchSearchService;
    private final CompanyElasticsearchSearchService companyElasticsearchSearchService;

    private static final int MAX_ALERT_SCORE = 160;

    // 1. searchJobs — AI tìm việc bằng ngôn ngữ tự nhiên
    public List<JobAIResponseDTO> searchJobs(
            String keyword,
            String location,
            Double salaryMin,
            Integer experienceYears,
            String level,
            String workMode,
            String workingType,
            String companyName,
            String categoryName) {

        JobLevel jobLevel = parseEnumSafe(JobLevel.class, level);
        WorkMode mode = parseEnumSafe(WorkMode.class, workMode);
        WorkingType type = parseEnumSafe(WorkingType.class, workingType);
        JobSearchRequestAI jobSearchRequestAI = JobSearchRequestAI.builder()
                .keyword(keyword)
                .location(location)
                .salaryMin(salaryMin)
                .experienceYears(experienceYears)
                .workMode(mode)
                .workingType(type)
                .companyName(companyName)
                .categoryName(categoryName)
                .build();
        ElasticsearchSearchResult result;
        try {
            result = elasticsearchSearchService.searchJobAI(jobSearchRequestAI);
            if (result == null || result.getIds() == null || result.getIds().isEmpty()) {
                return List.of();
            }

            List<Job> jobs = jobRepository.findByIdIn(result.getIds());
            List<Job> orderedJobs = reorderJobs(result.getIds(), jobs);
            log.info("Search used Elasticsearch successful");
            return orderedJobs.stream()
                    .map(jobMapper::searchJobAI)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Elasticsearch search failed. Fallback to database. " + "keyword={}", keyword, e);
            Specification<Job> specification =
                    Specification.where(JobSpecification.notDeleted())
                            .and(JobSpecification.activeOnly())
                            .and(JobSpecification.keyword(keyword))
                            .and(JobSpecification.hasLocation(location))
                            .and(JobSpecification.hasSalary(salaryMin, null))
                            .and(JobSpecification.hasExperience(experienceYears))
                            .and(JobSpecification.hasLevel(jobLevel))
                            .and(JobSpecification.hasWorkMode(mode))
                            .and(JobSpecification.hasWorkingType(type))
                            .and(JobSpecification.hasCompanyName(companyName))
                            .and(JobSpecification.companyActive())
                            .and(JobSpecification.hasCategory(categoryName));

            List<Job> jobs = jobRepository.findAll(specification);

            return jobs.stream()
                    .limit(10)
                    .map(jobMapper::searchJobAI)
                    .collect(Collectors.toList());
        }
    }

    // 2. getJobDetail — chi tiết 1 job (mô tả, yêu cầu tiếng Anh, ...)
    public JobAIDetailResponseDTO getJobDetail(Long jobId) {
        Job job = jobRepository.findByIdWithDetails(jobId).orElse(null);

        if (job == null || job.isDeleted()) {
            return null;
        }
        List<String> skills = job.getJobSkills().stream()
                .map(js -> js.getSkill().getName()
                        + (Boolean.TRUE.equals(js.getRequired())
                        ? " (bắt buộc)"
                        : " (ưu tiên)"))
                .collect(Collectors.toList());
        return jobMapper.detailJobForAI(job, skills);
    }


    //    3.search company
    public List<CompanyAiDetailDTO> searchCompanies(
            String name,
            String taxCode,
            String email,
            String phone,
            String website,
            String address,
            String industryName
    ) {
        ElasticsearchSearchResult result;
        try {
            SearchCompanyAI searchCompanyAI = SearchCompanyAI.builder()
                    .name(name)
                    .taxCode(taxCode)
                    .email(email)
                    .phone(phone)
                    .website(website)
                    .address(address)
                    .industryName(industryName)
                    .build();
            result = companyElasticsearchSearchService.searchCompanyForAi(searchCompanyAI);

            if (result == null || result.getIds() == null || result.getIds().isEmpty()) {
                log.warn("Elasticsearch search returned no results or null. Returning empty list.");
                return List.of();
            }
            List<Company> companies = companyRepository.findByIdIn(result.getIds());
            List<Company> orderedCompanies = reorderCompanies(result.getIds(), companies);

            List<CompanyAiDetailDTO> response = orderedCompanies.stream()
                    .map(companyMapper::toCompanyAiDetailDTO)
                    .collect(Collectors.toList());

            log.info("Successfully returned {} companies via Elasticsearch flow.", response.size());
            return response;

        } catch (Exception e) {
            log.error("Elasticsearch search failed with error: {}. Falling back to Database Specification search.", e.getMessage(), e);
            Specification<Company> specification =
                    Specification.where(CompanySpecification.visible())
                            .and(CompanySpecification.active())
                            .and(CompanySpecification.hasKeyword(name))
                            .and(CompanySpecification.hasTaxCode(taxCode))
                            .and(CompanySpecification.hasEmail(email))
                            .and(CompanySpecification.hasPhone(phone))
                            .and(CompanySpecification.hasWebsite(website))
                            .and(CompanySpecification.hasAddress(address))
                            .and(CompanySpecification.hasindustryName(industryName));

            List<Company> companies = companyRepository.findAll(specification);

            List<CompanyAiDetailDTO> response = companies.stream()
                    .limit(10)
                    .map(companyMapper::toCompanyAiDetailDTO)
                    .collect(Collectors.toList());

            log.info("Successfully returned {} companies via Database fallback flow.", response.size());
            return response;
        }
    }
    //    4.thống kế trạng thái úng tuyển
    public ApplicationSummaryDTO getApplicationSummary(Long userId) {
        Object[] result = applicationRepository.getApplicationSummary(userId);
        return ApplicationSummaryDTO.builder()
                .total((Long) result[0])
                .pending((Long) result[1])
                .reviewing((Long) result[2])
                .interviewing((Long) result[3])
                .offered((Long) result[4])
                .hired((Long) result[5])
                .rejected((Long) result[6])
                .latestAppliedAt((Instant) result[7])
                .build();
    }

    //    5.Search Application theo yeu cau
    public List<AppliedJobResponseAIDTO> getAppliedJobs(
            Long userId,
            String statusApplied,
            String keyword,
            Integer limit
    ) {
        if (limit == null || limit <= 0) {
            limit = 10;
        }
        Pageable pageable = PageRequest.of(0, limit, Sort.by("createdAt").descending());

        ResumeStatus status = parseEnumSafe(ResumeStatus.class, statusApplied);

        List<Application> applications = applicationRepository.searchApplications(userId, status, keyword, pageable);

        return applications.stream()
                .map(applicationMapper::toAppliedJobResponseAI)
                .toList();
    }

    // 6. saveFavoriteJob — lưu job yêu thích
    @Transactional
    public Map<String, Object> saveFavoriteJob(Long userId, Long jobId) {

        RLock lock = redissonClient.getLock(buildSaveFavoriteJobLock(userId, jobId));
        boolean acquired = false;
        try {
            acquired = lock.tryLock(5, 30, TimeUnit.SECONDS);
            if (!acquired) {
                throw new ResourceLockedException("Bạn đang thực hiện thao tác lưu công việc yêu thích. Vui lòng thử lại.");
            }
            if (savedJobRepository.existsByUserIdAndJobId(userId, jobId)) {
                return Map.of("status", "already_saved",
                        "message", "Công việc đã có trong danh sách yêu thích."
                );
            }
            Job job = jobRepository.findByIdAndDeletedFalse(jobId)
                    .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy công việc."));

            User user = userRepository.findByIdAndDeletedFalse(userId)
                    .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng."));

            SavedJob savedJob = new SavedJob();
            savedJob.setJob(job);
            savedJob.setUser(user);

            savedJobRepository.save(savedJob);
            return Map.of(
                    "status", "success",
                    "message", "Đã lưu công việc '" + job.getName() + "' vào danh sách yêu thích."
            );
        } catch (InterruptedException ex) {

            Thread.currentThread().interrupt();
            throw new AppException("Không thể lấy lock." , ex);

        } catch (DataIntegrityViolationException ex) {

            return Map.of("status", "already_saved",
                    "message", "Công việc đã có trong danh sách yêu thích."
            );
        } finally {

            if (acquired && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    // 7.findMatchingJobs
    public GeminiJobResponAI geminiJobResponAI(Long userId) {
        List<Skill> skills = getUserSkills(userId);
        List<String> nameSkills = skills.stream()
                .map(Skill::getName)
                .toList();
        return GeminiJobResponAI.builder()
                .userSkills(nameSkills)
                .jobs(findMatchingJobs(userId))
                .build();
    }

    //    8.removeFavoriteJob
    @Transactional
    public Map<String, Object> removeFavoriteJob(Long userId, Long jobId) {
        SavedJob savedJob = savedJobRepository.findByUserIdAndJobId(userId, jobId).orElse(null);
        if (savedJob == null) {
            return Map.of(
                    "status", "error",
                    "message", "Công việc này không có trong danh sách yêu thích."
            );
        }
        String jobName = savedJob.getJob().getName();
        savedJobRepository.delete(savedJob);
        return Map.of(
                "status", "success",
                "jobId", jobId,
                "message", "Đã bỏ lưu công việc '" + jobName + "' khỏi danh sách yêu thích."
        );
    }

    //    9.getFavoriteJobs
    public List<SavedJobResponse> getFavoriteJobs(Long userId, Integer limit) {
        if (limit == null || limit <= 0) {
            limit = 10;
        }

        Pageable pageable = PageRequest.of(
                0,
                limit,
                Sort.by("savedAt").descending()
        );

        Page<SavedJob> pageList = savedJobRepository.findByUserId(userId, pageable);
        return pageList.getContent().stream()
                .map(savedJobMapper::fromChatboxAI)
                .toList();
    }


    public List<JobRecommendationContext> findMatchingJobs(Long userId) {

        Map<Long, JobRecommendationScore> recommendationMap = new HashMap<>();

        List<Skill> skills = getUserSkills(userId);

        Set<Long> skillIds = skills.stream().map(Skill::getId)
                .collect(Collectors.toSet());

        List<JobAlert> alerts = jobAlertRepository.findByUserIdWithJobCategory(userId);

        List<SkillMatchResult> skillResults =
                jobRepository.findTop10BySkillsChatbox(skillIds, Instant.now(), PageRequest.of(0, 10));
        if (skills.isEmpty() && alerts.isEmpty()) {
            return List.of();
        }

        if (!skills.isEmpty()) {

            List<Long> jobIds = skillResults.stream()
                    .map(r -> r.job().getId())
                    .toList();

            Map<Long, Job> jobs = jobRepository.findByIdIn(jobIds)
                    .stream()
                    .collect(Collectors.toMap(Job::getId, Function.identity()));


            for (SkillMatchResult item : skillResults) {
                Job job = jobs.get(item.job().getId());

                JobRecommendationScore score = recommendationMap.computeIfAbsent(job.getId(), x -> new JobRecommendationScore(job));
                score.setSubSkillScore(calculateSubSkillScore(job, skillIds));

                job.getJobSkills().forEach(js -> {
                    if (skillIds.contains(js.getSkill().getId())) {
                        score.getMatchedSkills().add(js.getSkill().getName());
                    }
                });
                score.addReason("Job matches your skills");

            }
        }


        List<AlertMatchResult> allMatches = alerts.stream()
                .flatMap(alert -> jobRepository.findTopJobsForAlertChatbox(
                        alert.getKeyword(),
                        alert.getLocation(),
                        alert.getJobCategory() != null ? alert.getJobCategory().getId() : null,
                        alert.getLevel(),
                        alert.getWorkMode(),
                        alert.getSalaryMin(),
                        alert.getSalaryMax(),
                        alert.getWorkingType(),
                        PageRequest.of(0, 10)
                ).stream())
                .toList();

        List<Long> Ids = allMatches.stream()
                .map(r -> r.job().getId())
                .toList();

        Map<Long, Job> jobAlerts = jobRepository.findByIdIn(Ids)
                .stream()
                .collect(Collectors.toMap(Job::getId, Function.identity()));
        for (AlertMatchResult item : allMatches) {
            Job job = jobAlerts.get(item.job().getId());
            JobRecommendationScore score = recommendationMap.computeIfAbsent(
                    job.getId(),
                    x -> new JobRecommendationScore(job)
            );
            int alertScore = (int) Math.round(item.rawAlertScore() * 100.0 / MAX_ALERT_SCORE);
            score.setAlertScore(alertScore);
//            score.calculateTotalScore();
            score.addReason("Matches your Job Alert");
        }

        return recommendationMap.values()
                .stream()
                .sorted(Comparator.comparing(JobRecommendationScore::getScore).reversed())
                .limit(10)
                .map(this::convert)
                .toList();
    }

    private JobRecommendationContext convert(JobRecommendationScore data) {
        Job job = data.getJob();

//        int finalScore = Math.min(data.getScore(), 100);
        return new JobRecommendationContext(
                job.getId(),
                job.getName(),
                job.getCompany().getName(),
                job.getLocation(),
                job.getLevel().name(),
                job.getWorkMode().name(),
                job.getWorkingType().name(),
                job.getSalaryMin(),
                job.getSalaryMax(),
                data.getScore(),
                data.getMatchedSkills()
                        .stream()
                        .toList(),
                data.getReasons()
                        .stream()
                        .toList()

        );
    }


    private int calculateSubSkillScore(Job job, Set<Long> candidateSkillIds) {

        int totalWeight = 0;
        int matchedWeight = 0;

        for (JobSkill js : job.getJobSkills()) {

            int weight = Optional.ofNullable(js.getPriority()).orElse(1);

            // Required có trọng số cao hơn
            if (Boolean.TRUE.equals(js.getRequired())) {
                weight *= 2;
            }

            totalWeight += weight;

            if (candidateSkillIds.contains(js.getSkill().getId())) {
                matchedWeight += weight;
            }
        }

        if (totalWeight == 0) {
            return 0;
        }

        return (int) Math.round(matchedWeight * 100.0 / totalWeight);
    }

    private List<Skill> getUserSkills(Long userId) {

        return subscriberRepository.findByUserIdWithSkills(userId)
                .stream()
                .flatMap(s -> s.getSubscriberSkills().stream())
                .map(SubscriberSkill::getSkill)
                .distinct()
                .toList();

    }


    private String buildSaveFavoriteJobLock(Long userId, Long jobId) {
        return "favorite-job:" + userId + ":" + jobId;
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

    private List<Company> reorderCompanies(List<Long> ids, List<Company> companies) {

        Map<Long, Company> companyMap = companies.stream()
                .collect(Collectors.toMap(
                        Company::getId,
                        Function.identity()
                ));

        return ids.stream()
                .map(companyMap::get)
                .filter(Objects::nonNull)
                .toList();
    }

}

