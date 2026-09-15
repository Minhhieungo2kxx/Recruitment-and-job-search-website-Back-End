package com.webjob.application.service.JobRecommendation;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.webjob.application.dto.record.*;
import com.webjob.application.enums.RecommendationStatus;
import com.webjob.application.exception.Customs.BusinessException;
import com.webjob.application.exception.Customs.GeminiUnavailableException;
import com.webjob.application.exception.Customs.ResourceNotFoundException;
import com.webjob.application.mapper.ApplicationMapper;
import com.webjob.application.mapper.JobMapper;
import com.webjob.application.mapper.SavedJobMapper;
import com.webjob.application.models.Entity.*;
import com.webjob.application.repository.ApplicationRepository;
import com.webjob.application.repository.JobRecommendationRepository;
import com.webjob.application.repository.SavedJobRepository;
import com.webjob.application.repository.UserResumeRepository;
import com.webjob.application.component.SecurityUtils;
import com.webjob.application.service.Redis.RecommendationCacheService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class JobRecommendationService {
    private final CvExtractionService cvExtractionService;

    private final CandidateJobService candidateJobService;

    private final UserResumeRepository resumeRepository;

    private final SavedJobRepository savedJobRepository;

    private final ApplicationRepository applicationRepository;

    private final JobRecommendationRepository recommendationRepository;

    private final ObjectMapper objectMapper;

    private final SkillKeywordMatcher skillKeywordMatcher;

    private final SecurityUtils securityUtils;

    private final RecommendationCacheService recommendationCacheService;

    private final JobMapper jobMapper;

    private final ApplicationMapper applicationMapper;

    private final SavedJobMapper savedJobMapper;

    @Value("${gemini.api.key}")
    private String apiKeysRaw;

    @Value("${gemini.api.base-url}")
    private String baseUrl;

    @Value("${gemini.api.models}")
    private String modelsRaw;


    private final RestTemplate restTemplate = new RestTemplate();

    private final AtomicInteger keyRotationIndex = new AtomicInteger(0);


    private static final Duration CACHE_TTL = Duration.ofHours(24);
    private static final String RECOMMENDATION_SYSTEM_TEMPLATE = """
        Bạn là AI phân tích CV và gợi ý việc làm cho nền tảng tuyển dụng.

        NHIỆM VỤ:
        Dựa trên CV, lịch sử ứng tuyển/đã lưu và jobsToEvaluate, đánh giá độc lập
        mức độ phù hợp của TỪNG job trong jobsToEvaluate.

        QUY TẮC CHUNG:
        - Chỉ đánh giá job có trong jobsToEvaluate, không tạo hoặc bổ sung job khác.
        - Mỗi job phải có: matchScore, matchReason, matchedSkills, missingSkills.
        - matchScore: 0-100, phản ánh mức độ phù hợp thực tế.
        - Ưu tiên: skill match > kinh nghiệm > vị trí mong muốn > seniority > địa điểm.
        - Lịch sử ứng tuyển/đã lưu chỉ là tín hiệu về sở thích, không được tự tăng điểm
          nếu CV không phù hợp.

        ============================================================
        SKILL MATCHING - QUY TẮC QUAN TRỌNG NHẤT
        ============================================================

        Không so sánh skill bằng exact string. Phải đánh giá theo NGỮ NGHĨA
        và quan hệ kỹ thuật.

        Một requiredSkill được MATCHED khi CV có bằng chứng rõ ràng về:
        - chính skill đó;
        - tên gọi/viết tắt/phiên bản tương đương;
        - công nghệ cụ thể thuộc skill tổng quát;
        - hoặc một thành phần phù hợp trong skill dạng nhóm/lựa chọn.

        REQUIRED SKILL DẠNG NHÓM:
        Nếu requiredSkill chứa các lựa chọn như:
        - "A / B"
        - "A hoặc B"
        - "A (B / C)"
        - "A / B / C"

        thì chỉ cần CV có ÍT NHẤT MỘT thành phần phù hợp là MATCHED.
        Không yêu cầu CV phải có tất cả thành phần.

        Ví dụ BẮT BUỘC:
        CV: "Apache Kafka"
        requiredSkill: "Message Queue (RabbitMQ / Kafka)"
        => matchedSkills phải chứa chính xác:
           "Message Queue (RabbitMQ / Kafka)"
        => không được đưa skill này vào missingSkills.

        Tương tự:
        - "Kafka" -> MATCH "Message Queue (RabbitMQ / Kafka)"
        - "RabbitMQ" -> MATCH "Message Queue (RabbitMQ / Kafka)"
        - "Java 17" -> MATCH "Java"
        - "ReactJS" -> MATCH "React"
        - "Postgres" -> MATCH "PostgreSQL"

        Nhưng KHÔNG match chỉ vì hai công nghệ có liên quan:
        - MySQL != PostgreSQL
        - Redis != Kafka
        - Java != JavaScript
        - Docker != Kubernetes
        - REST API != GraphQL

        Chỉ MATCH khi quan hệ kỹ thuật đủ rõ ràng, trực tiếp hoặc có quan hệ
        bao hàm/tương đương rõ ràng. Không được tự suy diễn skill mà CV không
        thể hiện.

        ============================================================
        MATCHED SKILLS / MISSING SKILLS
        ============================================================

        matchedSkills và missingSkills CHỈ được chứa giá trị nguyên gốc
        xuất hiện trong requiredSkills của chính job.

        Không được:
        - đổi tên;
        - dịch;
        - viết tắt;
        - tạo synonym;
        - đưa skill từ CV trực tiếp vào output.

        Ví dụ:

        requiredSkills:
        ["Java", "Message Queue (RabbitMQ / Kafka)", "Docker"]

        CV:
        ["Java 17", "Apache Kafka"]

        Kết quả:
        matchedSkills:
        ["Java", "Message Queue (RabbitMQ / Kafka)"]

        missingSkills:
        ["Docker"]

        Mỗi requiredSkill phải xuất hiện ở ĐÚNG MỘT trong hai danh sách:
        matchedSkills hoặc missingSkills.

        Không được bỏ sót, trùng lặp hoặc thêm skill ngoài requiredSkills.

        ============================================================
        BẰNG CHỨNG TỪ CV
        ============================================================

        Chỉ coi skill là có trong CV khi có bằng chứng rõ ràng từ Skills,
        Technical Skills, Experience, Project, Technologies Used,
        Certification hoặc nội dung kỹ thuật tương đương.

        Không được suy diễn skill không có bằng chứng.

        Ví dụ CV có "Developed backend services using Spring Boot and Kafka"
        thì có bằng chứng cho Spring Boot và Kafka, nhưng không tự suy diễn
        RabbitMQ, Kubernetes hoặc AWS.

        ============================================================
        KINH NGHIỆM
        ============================================================

        So sánh số năm kinh nghiệm thực tế trong CV với yêu cầu của job.
        Nếu ít hơn yêu cầu thì giảm mức độ phù hợp tương ứng, không mặc định
        là không phù hợp. Không tự tạo số năm kinh nghiệm.

        ============================================================
        VỊ TRÍ VÀ SENIORITY
        ============================================================

        Đối chiếu desired position của ứng viên với job title và nội dung công việc,
        dựa trên ý nghĩa thực tế thay vì exact string.

        Ví dụ:
        "Backend Developer" phù hợp với "Java Backend Engineer".

        Đồng thời đối chiếu seniority/job level. Skill phù hợp nhưng seniority
        chênh lệch đáng kể phải được phản ánh trong matchScore.

        ============================================================
        ĐỊA ĐIỂM - ĐIỀU KIỆN BẮT BUỘC
        ============================================================

        Nếu xác định được tỉnh/thành phố của ứng viên:
        - Chỉ phù hợp với job có location tại đúng tỉnh/thành phố đó.
        - Job nhiều location chỉ phù hợp nếu có ít nhất một location trùng.
        - Job remote/toàn quốc chỉ phù hợp nếu job data ghi rõ ứng viên có thể
          làm việc từ địa điểm của mình.
        - Không tự suy diễn relocation hoặc khả năng làm việc xa.

        Nếu không xác định được địa điểm thì không tự suy diễn.

        ============================================================
        MATCH REASON
        ============================================================

        matchReason phải bằng tiếng Việt, tự nhiên, chuyên nghiệp và giải thích
        rõ lý do của matchScore trong 2-5 câu.

        Nêu các yếu tố thực sự có bằng chứng:
        - skill nổi bật và mức độ đáp ứng;
        - kinh nghiệm so với yêu cầu;
        - desired position;
        - seniority;
        - skill còn thiếu hoặc điểm hạn chế;
        - địa điểm nếu ảnh hưởng đến độ phù hợp;
        - kết luận tổng thể.

        Không chỉ liệt kê skill. Phải giải thích MỐI LIÊN HỆ giữa CV và job.

        Không nhắc đến prompt, system, quy tắc nội bộ, thuật toán hoặc quá trình
        suy luận. Không đưa thông tin không có trong CV/job data.

        ============================================================
        MATCH SCORE
        ============================================================

        Skill match là yếu tố quan trọng nhất.

        Tham chiếu:
        - 90-100: Phù hợp rất cao, đáp ứng gần như toàn bộ yêu cầu quan trọng.
        - 75-89: Phù hợp cao, còn một số thiếu hụt nhỏ.
        - 60-74: Phù hợp khá, còn khoảng cách đáng kể.
        - 40-59: Phù hợp thấp/trung bình, chỉ đáp ứng một phần.
        - 0-39: Ít phù hợp hoặc có không tương thích lớn.

        Đây là mức tham chiếu, không phải công thức cứng.
        Không cho điểm cao chỉ vì lịch sử ứng tuyển/đã lưu.

        ============================================================
        KIỂM TRA TRƯỚC KHI TRẢ KẾT QUẢ
        ============================================================

        Đảm bảo:
        - Chỉ trả job trong candidateJobs.
        - matchScore từ 0-100.
        - matchedSkills/missingSkills chỉ dùng giá trị nguyên gốc từ requiredSkills.
        - Mỗi requiredSkill thuộc đúng một danh sách.
        - Không bỏ sót requiredSkill.
        - Semantic matching đã được áp dụng.
        - Skill dạng nhóm chỉ cần một thành phần phù hợp để MATCH.
        - Không đánh dấu missing nếu CV đã thể hiện thành phần phù hợp.
        - matchReason giải thích cụ thể, tự nhiên và nhất quán với matchScore.
        - Tuân thủ điều kiện địa điểm.

        Chỉ trả về dữ liệu theo response schema được cung cấp.
        """;


    @Transactional
    public JobRecommendationResponse recommend() {
        User user = securityUtils.getCurrentUser();
        return doRecommend(user, false);
    }

    @Transactional
    public JobRecommendationResponse forceRefresh() {
        User user = securityUtils.getCurrentUser();
        return doRecommend(user, true);
    }

    public JobRecommendationResponse doRecommend(User user, boolean forceRefresh) {
        Long userId = user.getId();

//        NORMAL CACHE FLOW

        if (!forceRefresh) {
            JobRecommendationResponse cached = recommendationCacheService.get(userId);
            if (cached != null) {
                return cached;
            }
            JobRecommendationResponse fromDatabase = getFromDatabase(userId);
            if (fromDatabase != null) {
                recommendationCacheService.put(userId, fromDatabase);
                return fromDatabase;
            }
        }

//        SNAPSHOT VERSION,Ghi nhận version trước khi chờ lock.
        Long versionBeforeLock = recommendationCacheService.getVersion(userId);

//         DISTRIBUTED LOCK
        RLock lock = recommendationCacheService.getLock(userId);
        boolean acquired = false;
        try {
//           Không truyền leaseTime để Redisson watchdog quản lý thời gian lock.
            acquired = lock.tryLock(30, TimeUnit.SECONDS);

            if (!acquired) {
                throw new BusinessException("Hệ thống đang tạo gợi ý việc làm, " + "vui lòng thử lại sau");
            }

//          DOUBLE CHECK
            if (!forceRefresh) {
                JobRecommendationResponse cached = recommendationCacheService.get(userId);
                if (cached != null) {
                    return cached;
                }
            }
//            FORCE REFRESH SINGLE-FLIGHT

            if (forceRefresh) {

                Long currentVersion = recommendationCacheService.getVersion(userId);
                boolean anotherRequestAlreadyRefreshed =
                        !Objects.equals(
                                versionBeforeLock,
                                currentVersion
                        );

                if (anotherRequestAlreadyRefreshed) {

                    JobRecommendationResponse latest = recommendationCacheService.getLatest(userId);
                    if (latest != null) {
                        return latest;
                    }
                }
            }

//          GENERATE
            JobRecommendationResponse response = generateRecommendation(user);

//            CACHE
            recommendationCacheService.put(userId, response);
//
            return response;
//
        } catch (InterruptedException e) {

            Thread.currentThread().interrupt();
            throw new BusinessException("Không thể xử lý recommendation");

        } finally {

            if (acquired && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    private JobRecommendationResponse generateRecommendation(User user) {

        //Lấy CV mặc định
        UserResume defaultResume = resumeRepository.findByUserIdAndIsDefaultTrue(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Bạn chưa có CV mặc định"));

        // Trích xuất raw text CV,// Dùng full text để tìm skill
        String rawCvText = cvExtractionService.extractRawText(defaultResume.getUrl());

        // Lịch sử ứng tuyển
        List<Application> recentApplications = applicationRepository
                .findTop10ByUserIdOrderByCreatedAtDesc(user.getId());

//       Lịch sử saved job
        List<SavedJob> recentSavedJobs = savedJobRepository
                .findTop10ByUserIdOrderBySavedAtDesc(user.getId());

        // lọc candidate jobs bằng skill overlap thô (parse tên skill xuất hiện trong CV)
        List<String> roughSkillGuess = skillKeywordMatcher.extractKnownSkillNames(rawCvText);

        List<Job> candidateJobs = candidateJobService.findCandidates(user, roughSkillGuess);

        if (candidateJobs.isEmpty()) {
            throw new BusinessException("Hiện chưa có job phù hợp để gợi ý cho bạn");
        }

        // giới hạn text khi gửi cho AI
        String cvTextForAi = cvExtractionService.truncateForAi(rawCvText, 12000);

        //Build prompt (LLM tự trích xuất CV có cấu trúc + chấm điểm)
        Map<String, Object> userContent = buildPromptPayload(cvTextForAi, recentApplications, recentSavedJobs, candidateJobs);

        List<Map<String, Object>> contents = List.of(
                Map.of("role", "user",
                        "parts", List.of(
                                Map.of("text",
                                        toJson(userContent)
                                ))));

        // Gọi LLM Gemini (tái dùng runWithFallback đã có sẵn trong hệ thống)
        JsonNode result = callWithFallback(RECOMMENDATION_SYSTEM_TEMPLATE, contents, true);


        LLMRecommendationResult parsed;

        try {
            parsed = objectMapper.treeToValue(extractJsonText(result),
                    LLMRecommendationResult.class
            );
        } catch (JsonProcessingException e) {
            throw new GeminiUnavailableException("Không thể parse Gemini response", e);
        }

//        SAVE DB
        Instant now = Instant.now();

        JobRecommendation recommendation = JobRecommendation.builder()
                .user(user)
                .resume(defaultResume)
                .status(RecommendationStatus.COMPLETED)
                .modelUsed("gemini")
                .createdAt(now)
                .expiresAt(now.plus(CACHE_TTL))
                .build();

        Map<Long, Job> jobById = candidateJobs.stream()
                .collect(Collectors.toMap(Job::getId, j -> j,
                        (existing, replacement) -> existing));

        List<JobRecommendationItem> items = new ArrayList<>();
        int rank = 1;
        for (LLMJobMatch match : parsed.recommendations()
                .stream()
                .sorted(
                        Comparator.comparing(
                                        LLMJobMatch::matchScore)
                                .reversed())
                .toList()) {

            Job job = jobById.get(match.jobId());
            if (job == null) continue;
            items.add(JobRecommendationItem.builder()
                    .recommendation(recommendation)
                    .job(job)
                    .matchScore(BigDecimal.valueOf(match.matchScore()).setScale(2, RoundingMode.HALF_UP))
                    .matchReason(match.reason())
                    .matchedSkills(toJson(match.matchedSkills()))
                    .missingSkills(toJson(match.missingSkills()))
                    .rank(rank++)
                    .createdAt(Instant.now())
                    .build());
        }
        recommendation.setItems(items);

        recommendationRepository.save(recommendation);

        return toResponse(recommendation);
    }

    private JobRecommendationResponse getFromDatabase(Long userId) {
        Instant now = Instant.now();

        return recommendationRepository
                .findFirstByUserIdAndStatusAndExpiresAtAfterOrderByCreatedAtDesc(
                        userId,
                        RecommendationStatus.COMPLETED,
                        now
                )
                .map(this::toResponse)
                .orElse(null);
    }


    private Map<String, Object> buildPromptPayload(String cvTextForAi, List<Application> apps,
                                                   List<SavedJob> saved, List<Job> candidates) {
        apps = apps == null ? Collections.emptyList() : apps;
        saved = saved == null ? Collections.emptyList() : saved;
        candidates = candidates == null ? Collections.emptyList() : candidates;
        return Map.of(
                "cvRawText", Objects.requireNonNullElse(cvTextForAi, ""),
                "applicationHistory", apps.stream()
                        .map(applicationMapper::applicationHistoryDtoGemini)
                        .toList(),
                "savedJobs", saved.stream()
                        .map(savedJobMapper::savedJobDtoGemini)
                        .toList(),
                "jobsToEvaluate", candidates.stream()
                        .map(jobMapper::toGeminiJobDto)
                        .toList()
        );
    }


    private String toJson(Object object) {
        try {
            return objectMapper.writeValueAsString(object);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize object to JSON", e);
        }
    }


    private JobRecommendationResponse toResponse(
            JobRecommendation recommendation
    ) {
        List<JobMatchDto> items = recommendation.getItems()
                .stream()
                .map(this::toJobMatchDto)
                .filter(Objects::nonNull)
                .toList();

        return new JobRecommendationResponse(
                recommendation.getId(),
                recommendation.getCreatedAt(),
                items
        );
    }

    private JobMatchDto toJobMatchDto(JobRecommendationItem item
    ) {
        Job job = item.getJob();

        if (job == null) {
            return null;
        }

        Company company = job.getCompany();
        JobCategory category = job.getJobCategory();

        return new JobMatchDto(
                // Job
                job.getId(),
                job.getName(),

                // Company
                company != null ? company.getId() : null,
                company != null ? company.getName() : null,
                company != null ? company.getLogo() : null,

                // Match
                item.getMatchScore(),
                item.getMatchReason(),
                fromJson(item.getMatchedSkills()),
                fromJson(item.getMissingSkills()),

                // Job information
                job.getLocation(),
                job.getSalaryMin(),
                job.getSalaryMax(),
                job.isNegotiable(),
                job.getLevel(),
                job.getExperienceRequired(),
                job.getWorkingType(),
                job.getWorkMode(),

                // Category
                category != null ? category.getId() : null,
                category != null ? category.getName() : null,

                // Recruitment
                job.getQuantity(),
                job.getAppliedCount(),
                job.getCompetitionLevel(),

                // Time
                job.getStartDate(),
                job.getEndDate(),

                // Statistics
                job.getViewCount(),

                // Status
                job.getStatus(),

                // Metadata
                job.getCreatedAt()
        );
    }


    private List<String> fromJson(String json) {
        try {
            return objectMapper.readValue(json, new TypeReference<>() {
            });
        } catch (Exception e) {
            return List.of();
        }
    }

    public JsonNode callWithFallback(String systemInstruction, List<Map<String, Object>> contents, boolean expectJsonOnly) {
        List<String> models = Arrays.asList(modelsRaw.split(","));
        List<String> keys = Arrays.asList(apiKeysRaw.split(","));

        Exception lastException = null;

        for (String model : models) {
            int attempts = keys.size();
            for (int i = 0; i < attempts; i++) {
                String apiKey = nextKey(keys);
                try {
                    log.info("Gemini recommendation call: model={} key={}", model, maskKey(apiKey));
                    return callGeminiRaw(model, apiKey, systemInstruction, contents, expectJsonOnly);
                } catch (HttpClientErrorException e) {
                    lastException = e;
                    int status = e.getStatusCode().value();
                    if (status == 429 || status == 403) {
                        log.warn("Key {} failed ({}) -> next key", maskKey(apiKey), status);
                        continue;
                    }
                    if (status == 404) {
                        log.warn("Model {} not found -> next model", model);
                        break;
                    }
                    throw e;
                }
            }
        }
        throw new GeminiUnavailableException("Tất cả model/key Gemini đều không khả dụng", lastException);
    }

    private JsonNode callGeminiRaw(
            String model,
            String apiKey,
            String systemInstruction,
            List<Map<String, Object>> contents,
            boolean expectJsonOnly) {

        try {
            Map<String, Object> body = new HashMap<>();

            body.put(
                    "systemInstruction",
                    Map.of("parts", List.of(Map.of("text", systemInstruction)))
            );

            body.put("contents", contents);

            if (expectJsonOnly) {
                body.put(
                        "generationConfig",
                        Map.of(
                                "responseMimeType", "application/json",
                                "responseSchema", Map.of(
                                        "type", "OBJECT",
                                        "properties", Map.of(
                                                "recommendations", Map.of(
                                                        "type", "ARRAY",
                                                        "items", Map.of(
                                                                "type", "OBJECT",
                                                                "properties", Map.of(
                                                                        "jobId", Map.of(
                                                                                "type", "INTEGER"
                                                                        ),
                                                                        "matchScore", Map.of(
                                                                                "type", "NUMBER",
                                                                                "minimum", 0,
                                                                                "maximum", 100
                                                                        ),
                                                                        "reason", Map.of(
                                                                                "type", "STRING"
                                                                        ),
                                                                        "matchedSkills", Map.of(
                                                                                "type", "ARRAY",
                                                                                "items", Map.of(
                                                                                        "type", "STRING"
                                                                                )
                                                                        ),
                                                                        "missingSkills", Map.of(
                                                                                "type", "ARRAY",
                                                                                "items", Map.of(
                                                                                        "type", "STRING"
                                                                                )
                                                                        )
                                                                ),
                                                                "required", List.of(
                                                                        "jobId",
                                                                        "matchScore",
                                                                        "reason",
                                                                        "matchedSkills",
                                                                        "missingSkills"
                                                                )
                                                        )
                                                )
                                        ),
                                        "required", List.of("recommendations")
                                )
                        )
                );
            }

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("X-goog-api-key", apiKey);

            HttpEntity<Map<String, Object>> request =
                    new HttpEntity<>(body, headers);

            String url = baseUrl + "/" + model + ":generateContent";

            ResponseEntity<String> response =
                    restTemplate.postForEntity(url, request, String.class);

            return objectMapper.readTree(response.getBody());

        } catch (HttpClientErrorException e) {
            throw e;

        } catch (JsonProcessingException e) {
            throw new GeminiUnavailableException("Gemini trả về JSON không hợp lệ", e);
        } catch (RestClientException e) {
            throw new GeminiUnavailableException("Không thể gọi Gemini API", e);
        }
    }


    private String nextKey(List<String> keys) {
        int idx = Math.floorMod(keyRotationIndex.getAndIncrement(), keys.size());
        return keys.get(idx).trim();
    }

    private String maskKey(String key) {
        if (key == null || key.length() < 8) return "****";
        return key.substring(0, 4) + "..." + key.substring(key.length() - 4);
    }

    private JsonNode extractJsonText(JsonNode geminiResponse) {
        JsonNode candidates = geminiResponse.path("candidates");

        if (!candidates.isArray() || candidates.isEmpty()) {
            String blockReason = geminiResponse
                    .path("promptFeedback")
                    .path("blockReason")
                    .asText(null);

            if (blockReason != null) {
                throw new GeminiUnavailableException("Gemini từ chối xử lý request: " + blockReason, null);
            }

            throw new GeminiUnavailableException("Gemini không trả về candidates nào", null);
        }

        JsonNode firstCandidate = candidates.get(0);

        String finishReason = firstCandidate
                .path("finishReason")
                .asText("");

        if ("SAFETY".equals(finishReason) || "RECITATION".equals(finishReason)) {

            throw new GeminiUnavailableException("Gemini chặn nội dung do finishReason=" + finishReason, null);
        }

        String rawText = firstCandidate
                .path("content")
                .path("parts")
                .path(0)
                .path("text")
                .asText(null);

        if (rawText == null || rawText.isBlank()) {
            throw new GeminiUnavailableException("Gemini trả về nội dung rỗng", null);
        }

        String cleaned = rawText.trim()
                .replaceAll("^```json\\s*", "")
                .replaceAll("^```\\s*", "")
                .replaceAll("```$", "")
                .trim();

        try {
            return objectMapper.readTree(cleaned);
        } catch (JsonProcessingException e) {
            throw new GeminiUnavailableException("Gemini trả về JSON không hợp lệ", e);
        }
    }


}




