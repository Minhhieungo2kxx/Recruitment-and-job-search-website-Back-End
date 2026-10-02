package com.webjob.application.service.JobMatchingHR.Matching;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.webjob.application.component.SecurityUtils;
import com.webjob.application.dto.Request.ScoringResult;
import com.webjob.application.dto.Request.Search.MatchFilterRequest;
import com.webjob.application.dto.Request.Search.SubscriberFilterRequest;
import com.webjob.application.dto.Request.SkillMatchOutcome;
import com.webjob.application.dto.Response.MatchResultDto;
import com.webjob.application.dto.Response.MetaDTO;
import com.webjob.application.dto.Response.ResponseDTO;
import com.webjob.application.dto.Response.SubscriberListResponse;
import com.webjob.application.dto.record.CandidateProfileAiResult;
import com.webjob.application.dto.record.ExplanationResult;
import com.webjob.application.enums.MatchStatus;
import com.webjob.application.enums.ParseStatus;
import com.webjob.application.exception.Customs.CvProcessingException;
import com.webjob.application.exception.Customs.ForbiddenException;
import com.webjob.application.exception.Customs.ResourceNotFoundException;
import com.webjob.application.mapper.MatchResultMapper;
import com.webjob.application.models.Entity.*;
import com.webjob.application.repository.*;
import com.webjob.application.service.JobMatchingHR.Ai.CandidateProfileAiService;
import com.webjob.application.service.JobMatchingHR.Ai.MatchExplanationAiService;
import com.webjob.application.service.JobRecommendation.CvExtractionService;
import com.webjob.application.service.Specification.SubscriberSpecification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;


@Service
@RequiredArgsConstructor
@Slf4j
public class CvJobMatchingService {
    private final ApplicationRepository applicationRepository;
    private final JobSkillRepository jobSkillRepository;
    private final CandidateProfileRepository candidateProfileRepository;
    private final CandidateSkillRepository candidateSkillRepository;
    private final MatchResultRepository matchResultRepository;
    private final MatchResultSkillDetailRepository matchResultSkillDetailRepository;

    private final CvExtractionService cvExtractionService;
    private final CandidateProfileAiService candidateProfileAiService;
    private final SkillNormalizationService skillNormalizationService;
    private final MatchingScoringEngine matchingScoringEngine;
    private final MatchExplanationAiService matchExplanationAiService;

    private final ObjectMapper objectMapper;

    private final MatchResultMapper matchResultMapper;
    private final JobRepository jobRepository;
    private final SecurityUtils securityUtils;


    /**
     * Chỉ chấm điểm (Java, không gọi AI explanation) - dùng cho chạy hàng loạt theo Job.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void runMatchingForApplication(Long applicationId) {
        Application application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new IllegalArgumentException("Application không tồn tại: " + applicationId));

        Job job = application.getJob();

        MatchResult matchResult = matchResultRepository.findByApplicationId(applicationId)
                .orElseGet(() -> MatchResult.builder().application(application).build());
        matchResult.setStatus(MatchStatus.PROCESSING);
        matchResult = matchResultRepository.save(matchResult);

        try {
            CandidateProfile profile = getOrCreateCandidateProfile(application);

            List<JobSkill> jobSkills = jobSkillRepository.findByJobId(job.getId());
            ScoringResult scoringResult = matchingScoringEngine.score(job, jobSkills, profile);

            applyScoreFields(matchResult, scoringResult);
            matchResult.setStatus(MatchStatus.COMPLETED);
            matchResult.setMatchedAt(Instant.now());
            matchResult.setErrorMessage(null);
            // Xoá explanation cũ nếu đây là rerun (điểm/skill thay đổi -> giải thích cũ có thể sai lệch,
            // để lần xem chi tiết tiếp theo tự gọi AI sinh lại - xem ensureExplanation).
            clearExplanation(matchResult);

            replaceSkillDetails(matchResult, scoringResult.getSkillOutcomes());

            matchResultRepository.save(matchResult);

            log.info("Successfully completed matching process for application ID: {}", applicationId);

        } catch (Exception e) {
            log.error("Matching thất bại cho application {}", applicationId, e);
            matchResult.setStatus(MatchStatus.FAILED);
            matchResult.setErrorMessage(e.getMessage());
            matchResultRepository.save(matchResult);
        }
    }

    /**
     * Sinh explanation bằng AI CHỈ KHI HR thực sự mở xem chi tiết 1 candidate
     * (gọi từ HrMatchingController.getDetail). Nếu đã sinh trước đó (overallReason
     * đã có) thì trả lại luôn, KHÔNG gọi lại AI - idempotent, xem nhiều lần không tốn thêm.
     */
    @Transactional
    public MatchResult ensureExplanation(Long applicationId) {

        MatchResult matchResult = matchResultRepository.findWithSkillDetailsByApplicationId(applicationId)
                .orElseThrow(() -> new IllegalArgumentException("Chưa có kết quả matching cho application " + applicationId));

        checkpermissionHR(matchResult.getApplication().getJob().getId());
        if (matchResult.getStatus() != MatchStatus.COMPLETED) {
            // Chưa chấm điểm xong (PENDING/PROCESSING/FAILED) - không có gì để giải thích
            return matchResult;
        }

        if (matchResult.getOverallReason() != null) {

            log.debug("Explanation already exists for applicationId={}, skipping AI generation.", applicationId);
            return matchResult;
        }

        Application application = matchResult.getApplication();
        Job job = application.getJob();
        CandidateProfile profile = candidateProfileRepository.findWithSkillsByResume_Id(application.getResume().getId())
                .orElseThrow(() -> new IllegalStateException("CandidateProfile không tồn tại dù MatchResult đã COMPLETED"));

        ScoringResult scoringResult = rebuildScoringResult(matchResult);


        log.info("Triggering AI service to generate explanation for applicationId={}", applicationId);
        ExplanationResult explanation = matchExplanationAiService.explain(job, profile, scoringResult);

        applyExplanationFields(matchResult, explanation);

        MatchResult savedResult = matchResultRepository.save(matchResult);
        log.info("Successfully generated and saved AI explanation for applicationId={}", applicationId);
        return savedResult;
    }

    /**
     * Dựng lại ScoringResult từ dữ liệu ĐÃ LƯU (không tính toán lại) - dùng làm input cho AI explanation.
     */
    private ScoringResult rebuildScoringResult(MatchResult matchResult) {

        List<SkillMatchOutcome> outcomes = matchResult.getSkillDetails().stream()
                .map(d -> SkillMatchOutcome.builder()
                                .jobSkill(d.getJobSkill())
                                .candidateSkill(d.getCandidateSkill())
                                .matchType(d.getMatchType())
                                .note(d.getNote())
                                .build()

                        )
                .toList();

        return new ScoringResult(
                matchResult.getMatchScore(),
                matchResult.getSkillScore(),
                matchResult.getExperienceScore(),
                matchResult.getEducationScore(),
                matchResult.getProjectScore(),
                matchResult.getCertificationScore(),
                outcomes
        );
    }


    /**
     * Parse CV lần đầu (nếu chưa có profile) hoặc tái sử dụng profile đã parse trước đó
     */
    private CandidateProfile getOrCreateCandidateProfile(Application application) {
        Long resumeId = application.getResume().getId();

        return candidateProfileRepository.findWithSkillsByResume_Id(resumeId)
                .filter(p -> p.getParseStatus() == ParseStatus.SUCCESS)
                .orElseGet(() -> parseAndPersistProfile(application));
    }

    private CandidateProfile parseAndPersistProfile(Application application) {
        CandidateProfile profile = candidateProfileRepository
                .findByResume_Id(application.getResume().getId())
                .orElseGet(() -> CandidateProfile.builder()
                        .resume(application.getResume())
                        .build());

        try {
            // 1. Extract CV
            String rawCvText = cvExtractionService.extractRawText(
                    application.getResume().getUrl()
            );
            // 2. Truncate trước khi gửi AI
            String cvTextForAi = cvExtractionService.truncateForAi(
                    rawCvText,
                    12000
            );

//             Lưu raw text ngay sau khi extract
            profile.setRawText(cvTextForAi);


            // 3. Parse AI
            CandidateProfileAiResult aiResult = candidateProfileAiService.parse(cvTextForAi);

            // 4. Map profile information
            profile.setSummary(aiResult.summary());
            profile.setTotalExperienceYears(aiResult.totalExperienceYears());

            profile.setAiModel(aiResult.aiModel());
            profile.setAiPromptVersion(aiResult.aiPromptVersion());

            profile.setParseStatus(ParseStatus.SUCCESS);
            profile.setParseError(null);
            profile.setParsedAt(Instant.now());

            // 5. Replace experiences
            profile.getExperiences().clear();

            aiResult.details().getExperience().forEach(experience -> {
                CandidateExperience entity = CandidateExperience.builder()
                        .candidateProfile(profile)
                        .company(experience.getCompany())
                        .title(experience.getTitle())
                        .startDate(experience.getStartDate())
                        .endDate(experience.getEndDate())
                        .current(experience.isCurrent())
                        .description(experience.getDescription())
                        .durationMonths(experience.getDurationMonths())
                        .build();

                profile.getExperiences().add(entity);
            });

            // 6. Replace educations
            profile.getEducations().clear();

            aiResult.details().getEducation().forEach(education -> {
                CandidateEducation entity = CandidateEducation.builder()
                        .candidateProfile(profile)
                        .school(education.getSchool())
                        .degree(education.getDegree())
                        .major(education.getMajor())
                        .startDate(education.getStartDate())
                        .endDate(education.getEndDate()) // FIX
                        .gpa(education.getGpa())
                        .relevance(education.getRelevance())
                        .build();

                profile.getEducations().add(entity);
            });

            // 7. Replace projects
            profile.getProjects().clear();

            aiResult.details().getProjects().forEach(project -> {
                CandidateProject entity = CandidateProject.builder()
                        .candidateProfile(profile)
                        .name(project.getName())
                        .description(project.getDescription())
                        .technologies(project.getTechStack())
                        .role(project.getRole())
                        .relevance(project.getRelevance())
                        .build();

                profile.getProjects().add(entity);
            });

            // 8. Replace certifications
            profile.getCertifications().clear();

            aiResult.details().getCertifications().forEach(certification -> {
                CandidateCertification entity = CandidateCertification.builder()
                        .candidateProfile(profile)
                        .name(certification.getName())
                        .issuer(certification.getIssuer())
                        .issueDate(certification.getIssueDate())
                        .expireDate(certification.getExpireDate())
                        .build();

                profile.getCertifications().add(entity);
            });

            // 9. Save profile + children
            CandidateProfile saved = candidateProfileRepository.save(profile);

            // 10. Replace skills
            saved.getSkills().clear();

            List<CandidateSkill> skills = skillNormalizationService.normalize(profile, aiResult.parsedSkills());

            saved.getSkills().addAll(skills);

            // CascadeType.ALL sẽ persist skills
            return candidateProfileRepository.save(saved);

        } catch (CvProcessingException e) {

            profile.setParseStatus(ParseStatus.FAILED);
            profile.setParseError(e.getMessage());
            profile.setParsedAt(Instant.now());

            candidateProfileRepository.save(profile);

            throw e;
        }
    }

    private void applyScoreFields(MatchResult matchResult, ScoringResult scoringResult) {
        matchResult.setMatchScore(scoringResult.getMatchScore());
        matchResult.setSkillScore(scoringResult.getSkillScore());
        matchResult.setExperienceScore(scoringResult.getExperienceScore());
        matchResult.setEducationScore(scoringResult.getEducationScore());
        matchResult.setProjectScore(scoringResult.getProjectScore());
        matchResult.setCertificationScore(scoringResult.getCertificationScore());
    }

    private void applyExplanationFields(MatchResult matchResult, ExplanationResult explanation) {
        matchResult.setStrengthsJson(toJson(explanation.strengths()));
        matchResult.setConcernsJson(toJson(explanation.concerns()));
        matchResult.setInterviewFocus(toJson(explanation.decisionSupport().interviewFocus()));
        matchResult.setVerificationItems(toJson(explanation.decisionSupport().verificationItems()));
        matchResult.setConsiderations(toJson(explanation.decisionSupport().considerations()));
        matchResult.setExperienceAssessment(explanation.experienceAssessment());
        matchResult.setEducationAssessment(explanation.educationAssessment());
        matchResult.setOverallReason(explanation.overallReason());
        matchResult.setAiModel(explanation.aiModel());
        matchResult.setAiPromptVersion(explanation.aiPromptVersion());
    }

    private void clearExplanation(MatchResult matchResult) {
        matchResult.setStrengthsJson(null);
        matchResult.setConcernsJson(null);
        matchResult.setExperienceAssessment(null);
        matchResult.setEducationAssessment(null);
        matchResult.setOverallReason(null);
        matchResult.setConsiderations(null);
        matchResult.setVerificationItems(null);
        matchResult.setInterviewFocus(null);
        matchResult.setAiModel(null);
        matchResult.setAiPromptVersion(null);
    }

    private void replaceSkillDetails(
            MatchResult matchResult,
            List<SkillMatchOutcome> outcomes
    ) {
        matchResult.clearSkillDetails();

        for (SkillMatchOutcome outcome : outcomes) {
            MatchResultSkillDetail detail = MatchResultSkillDetail.builder()
                    .matchResult(matchResult)
                    .jobSkill(outcome.getJobSkill())
                    .candidateSkill(outcome.getCandidateSkill())
                    .matchType(outcome.getMatchType())
                    .note(outcome.getNote())
                    .build();

            matchResult.addSkillDetail(detail);
        }
    }



    @Transactional(readOnly = true)
    public ResponseDTO<List<MatchResultDto.Summary>> getJobMatches(int page, int size, Long jobId, MatchFilterRequest request) {
        if (request == null) {
            request = new MatchFilterRequest();
        }
        checkpermissionHR(jobId);

        size = Math.min(Math.max(size, 1), 50);
        page = Math.max(page, 1);

        Pageable pageable = PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "matchScore"));
        Page<MatchResult> results = matchResultRepository.findByJobIdWithFilter(
                jobId, request.getMinScore(), request.getMaxScore(),
                request.getMatchStatus(), request.getResumeStatus(), pageable
        );

        int currentpage = results.getNumber() + 1;

        int pagesize = results.getSize();
        int totalpage = results.getTotalPages();
        Long totalItem = results.getTotalElements();

        MetaDTO metaDTO = new MetaDTO(currentpage, pagesize, totalpage, totalItem);
        List<MatchResultDto.Summary> list = results.getContent().stream()
                .map(matchResultMapper::toSummary)
                .toList();
        // 4. Trả về kết quả
        return new ResponseDTO<>(metaDTO, list);

    }


    private String toJson(List<String> list) {
        try {
            return objectMapper.writeValueAsString(list == null ? List.of() : list);
        } catch (Exception e) {
            return "[]";
        }
    }

    public void checkpermissionHR(Long jobId) {
        User user = securityUtils.getCurrentUser();

        if (!jobRepository.existsByIdAndCompanyId(jobId, user.getCompany().getId())) {
            throw new ForbiddenException("You are not allowed to access this job.");
        }
    }


}

