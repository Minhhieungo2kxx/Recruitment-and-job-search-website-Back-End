package com.webjob.application.service.JobMatchingHR.Matching;

import com.webjob.application.component.MatchingScoringProperties;
import com.webjob.application.dto.Request.ScoringResult;
import com.webjob.application.dto.Request.SkillMatchOutcome;
import com.webjob.application.enums.MatchType;
import com.webjob.application.enums.SkillLevel;
import com.webjob.application.enums.SkillSource;
import com.webjob.application.enums.SkillStatus;
import com.webjob.application.models.Entity.*;
import com.webjob.application.utils.common.UtilFormat;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Chấm điểm CV so với Job theo công thức trọng số cố định.
 * KHÔNG gọi AI ở tầng này - toàn bộ input (candidate skills đã chuẩn hoá,
 * nhãn relevance của education/project do AI gán trước đó) đã có sẵn.
 * Đảm bảo: cùng input -> luôn cùng output (deterministic, testable, audit được).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MatchingScoringEngine {
    private static final double RELEVANT = 100.0;
    private static final double PARTIAL = 50.0;
    private static final double NOT_RELEVANT = 0.0;
    /**
     * Không có dữ liệu (vd CV không có mục Education) -> điểm trung tính, không phạt oan
     */
    private static final double NEUTRAL_WHEN_EMPTY = 50.0;

    private final MatchingScoringProperties properties;

    public ScoringResult score(Job job, List<JobSkill> jobSkills, CandidateProfile candidateProfile) {
        List<CandidateSkill> candidateSkills = candidateProfile.getSkills();

        SkillScoringOutcome skillOutcome = scoreSkills(jobSkills, candidateSkills);
        log.info("Diem so skill la: [{}]", skillOutcome.skillScore);

        double experienceScore = scoreExperience(candidateProfile, job.getExperienceRequired());
        double educationScore = scoreByRelevance(
                candidateProfile.getEducations(),
                CandidateEducation::getRelevance
        );
        double projectScore = scoreByRelevance(
                candidateProfile.getProjects(),
                CandidateProject::getRelevance
        );
        double certificationScore = candidateProfile.getCertifications().isEmpty()
                ? 0.0
                : 100.0;

        MatchingScoringProperties.Weights w = properties.getWeights();
        double matchScore = round2(
                w.getSkill() * skillOutcome.skillScore
                        + w.getExperience() * experienceScore
                        + w.getEducation() * educationScore
                        + w.getProject() * projectScore
                        + w.getCertification() * certificationScore
        );

        return new ScoringResult(
                matchScore,
                round2(skillOutcome.skillScore),
                round2(experienceScore),
                round2(educationScore),
                round2(projectScore),
                round2(certificationScore),
                skillOutcome.outcomes
        );
    }


    private SkillScoringOutcome scoreSkills(
            List<JobSkill> jobSkills,
            List<CandidateSkill> candidateSkills
    ) {

        List<JobSkill> requiredSkills = jobSkills.stream()
                .filter(JobSkill::getRequired)
                .toList();

        List<JobSkill> preferredSkills = jobSkills.stream()
                .filter(js -> !js.getRequired())
                .toList();

        // ============================================================
        // 1. Index CandidateSkill theo Skill ID
        // ============================================================
        Map<Long, CandidateSkill> candidateBySkillId =
                candidateSkills.stream()
                        .filter(cs -> cs.getSkill() != null)
                        .filter(cs -> cs.getSkill().getId() != null)
                        .collect(Collectors.toMap(
                                cs -> cs.getSkill().getId(),
                                Function.identity(),
                                (a, b) -> a
                        ));

        // ============================================================
        // 2. Index theo normalized Skill name
        // ============================================================
        Map<String, CandidateSkill> candidateByNormalizedNameSkill =
                candidateSkills.stream()
                        .filter(cs -> cs.getSkill() != null)
                        .filter(cs -> cs.getSkill().getName() != null)
                        .collect(Collectors.toMap(
                                cs -> UtilFormat.normalize(
                                        cs.getSkill().getName()
                                ),
                                Function.identity(),
                                (a, b) -> a
                        ));

        // ============================================================
        // 3. Index theo raw text
        // ============================================================
        Map<String, CandidateSkill> candidateByNormalizedRawName =
                candidateSkills.stream()
                        .filter(cs -> cs.getRawText() != null)
                        .collect(Collectors.toMap(
                                cs -> UtilFormat.normalize(
                                        cs.getRawText()
                                ),
                                Function.identity(),
                                (a, b) -> a
                        ));

        List<SkillMatchOutcome> outcomes = new ArrayList<>();

        // ============================================================
        // 4. REQUIRED SKILLS
        // ============================================================

        double requiredWeightedScore = 0.0;
        double requiredWeight = 0.0;

        for (JobSkill js : requiredSkills) {

            CandidateSkill candidateSkill =
                    findMatchingCandidateSkill(
                            js,
                            candidateBySkillId,
                            candidateByNormalizedNameSkill,
                            candidateByNormalizedRawName
                    );

            SkillMatchOutcome outcome = evaluate(js, candidateSkill);

            outcomes.add(outcome);

            double weight = outcome.getWeight();

            requiredWeight += weight;

            requiredWeightedScore += outcome.getFinalScore() * weight;
        }

        // ============================================================
        // 5. PREFERRED SKILLS
        // ============================================================

        double preferredWeightedScore = 0.0;
        double preferredWeight = 0.0;

        for (JobSkill js : preferredSkills) {

            CandidateSkill candidateSkill =
                    findMatchingCandidateSkill(
                            js,
                            candidateBySkillId,
                            candidateByNormalizedNameSkill,
                            candidateByNormalizedRawName
                    );

            SkillMatchOutcome outcome =
                    evaluate(js, candidateSkill);

            outcomes.add(outcome);

            double weight = outcome.getWeight();

            preferredWeight += weight;

            preferredWeightedScore +=
                    outcome.getFinalScore() * weight;
        }

        // ============================================================
        // 6. REQUIRED RATIO
        // ============================================================

        double requiredRatio =
                requiredWeight == 0.0
                        ? 1.0
                        : requiredWeightedScore / requiredWeight;

        // ============================================================
        // 7. PREFERRED RATIO
        // ============================================================

        double preferredRatio =
                preferredWeight == 0.0
                        ? 0.0
                        : preferredWeightedScore / preferredWeight;

        // ============================================================
        // 8. REQUIRED SCORE + HARD GATE
        // ============================================================

        double skillBaseScore;

        if (requiredSkills.isEmpty()) {

            // Không có required skill
            skillBaseScore = 100.0;

        } else {

            double requiredMissingRatio =
                    1.0 - requiredRatio;

            if (requiredMissingRatio >
                    properties.getRequiredSkillMissingGateRatio()) {

                skillBaseScore =
                        100.0
                                * requiredRatio
                                * properties
                                .getRequiredSkillGatePenaltyFactor();

            } else {

                skillBaseScore =
                        100.0 * requiredRatio;
            }
        }

        // ============================================================
        // 9. PREFERRED BONUS
        // ============================================================

        double preferredBonus =
                preferredSkills.isEmpty()
                        ? 0.0
                        : properties.getPreferredSkillMaxBonus()
                        * preferredRatio;

        // ============================================================
        // 10. FINAL SKILL SCORE
        // ============================================================

        double skillScore =
                Math.min(
                        100.0,
                        skillBaseScore * 0.8
                                + preferredBonus
                );

        return new SkillScoringOutcome(
                skillScore,
                outcomes
        );
    }


    private CandidateSkill findMatchingCandidateSkill(
            JobSkill jobSkill,
            Map<Long, CandidateSkill> candidateBySkillId,
            Map<String, CandidateSkill> candidateByNormalizedNameSkill,
            Map<String, CandidateSkill> candidateByNormalizedRawName
    ) {
        Skill jobSkillEntity = jobSkill.getSkill();

        if (jobSkillEntity == null) {
            log.debug("JobSkill entity is null, skipping matching.");
            return null;
        }

        /*
         * ============================================================
         * CASE 1: Match trực tiếp bằng Skill ID
         * ============================================================
         */
        if (jobSkillEntity.getId() != null) {
            CandidateSkill directMatch = candidateBySkillId.get(jobSkillEntity.getId());
            if (directMatch != null) {
                log.debug("Matched candidate skill by ID [{}] for job skill: {}", jobSkillEntity.getId(), jobSkillEntity.getName());
                return directMatch;
            }
        }

        /*
         * ============================================================
         * CASE 2: Match bằng tên Skill chính
         * ============================================================
         */
        String normalizedJobSkillName = UtilFormat.normalize(jobSkillEntity.getName());
        CandidateSkill nameMatch = candidateByNormalizedNameSkill.get(normalizedJobSkillName);

        if (nameMatch != null) {
            log.debug("Matched candidate skill by normalized name [{}] for job skill: {}", normalizedJobSkillName, jobSkillEntity.getName());
            return nameMatch;
        }

        /*
         * ============================================================
         * CASE 3: Match bằng Skill Alias
         * ============================================================
         */
        if (jobSkillEntity.getAliases() != null) {
            for (SkillAlias alias : jobSkillEntity.getAliases()) {
                if (alias == null || alias.getStatus() != SkillStatus.ACTIVE) {
                    continue;
                }

                String normalizedAlias = alias.getNormalizedAlias();
                if (normalizedAlias == null || normalizedAlias.isBlank()) {
                    continue;
                }

                CandidateSkill aliasMatch = candidateByNormalizedRawName.get(normalizedAlias);
                if (aliasMatch != null) {
                    log.debug("Matched candidate skill by alias [{}] for job skill: {}", normalizedAlias, jobSkillEntity.getName());
                    return aliasMatch;
                }
            }
        }

        /*
         * Không match được.
         */
        log.debug("No matching candidate skill found for job skill ID: {}, Name: {}", jobSkillEntity.getId(), jobSkillEntity.getName());
        return null;
    }


    private SkillMatchOutcome evaluate(
            JobSkill jobSkill,
            CandidateSkill candidateSkill
    ) {

        double weight = resolvePriority(jobSkill);

        /*
         * ============================================================
         * CASE 1: Không tìm thấy skill
         * ============================================================
         */
        if (candidateSkill == null) {

            return new SkillMatchOutcome(
                    jobSkill,
                    null,
                    MatchType.MISSING,
                    "Không tìm thấy '%s' trong CV"
                            .formatted(jobSkill.getSkill().getName()),
                    0.0,  // matchScore
                    0.0,  // experienceScore
                    0.0,  // levelScore
                    0.0,  // confidenceScore
                    weight,
                    0.0   // finalScore
            );
        }

        /*
         * ============================================================
         * CASE 2: Skill tồn tại
         * ============================================================
         */

        double matchScore = 1.0;
        /*
         * Experience
         */
        double experienceScore = calculateExperienceScore(jobSkill, candidateSkill);

        /*
         * Level
         */
        double levelScore = calculateLevelScore(jobSkill, candidateSkill);

        /*
         * Confidence
         */
        double confidenceScore = calculateConfidenceScore(candidateSkill);


// * FINAL SCORE
// *
// * Weighted formula:
// *
// * 40% × skill match
// * 30% × experience
// * 20% × level
// * 10% × confidence
// *
// * Tất cả score đều nằm trong khoảng 0 -> 1.
// * ============================================================
// */
        double finalScore =
                0.40 * matchScore
                        + 0.30 * experienceScore
                        + 0.20 * levelScore
                        + 0.10 * confidenceScore;



        /*
         * ============================================================
         * Xác định MATCHED / PARTIAL
         * ============================================================
         */
        boolean experienceOk = experienceScore >= 1.0;

        boolean levelOk = levelScore >= 1.0;

        MatchType matchType;

        String note;

        if (experienceOk && levelOk) {
            matchType = MatchType.MATCHED;

            note = "Đáp ứng đầy đủ '%s', bao gồm kinh nghiệm và level yêu cầu"
                    .formatted(jobSkill.getSkill().getName());

        } else {

            matchType = MatchType.PARTIAL;

            if (!experienceOk && !levelOk) {

                note = "Có '%s' nhưng thiếu kinh nghiệm và chưa đạt level"
                        .formatted(jobSkill.getSkill().getName());

            } else if (!experienceOk) {

                note = "Có '%s' nhưng thiếu kinh nghiệm (%s/%s năm)"
                        .formatted(
                                jobSkill.getSkill().getName(),
                                formatExperience(
                                        candidateSkill.getYearsOfExperience()
                                ),
                                formatExperience(
                                        jobSkill.getExperienceYear()
                                )
                        );

            } else {

                note = "Có '%s' nhưng chưa đạt level yêu cầu (%s < %s)"
                        .formatted(
                                jobSkill.getSkill().getName(),
                                candidateSkill.getLevel(),
                                jobSkill.getLevel()
                        );
            }
        }

        return new SkillMatchOutcome(
                jobSkill,
                candidateSkill,
                matchType,
                note,
                matchScore,
                experienceScore,
                levelScore,
                confidenceScore,
                weight,
                finalScore
        );
    }

    private double calculateExperienceScore(
            JobSkill jobSkill,
            CandidateSkill candidateSkill
    ) {

        Integer requiredExperience = jobSkill.getExperienceYear();

        /*
         * Job không yêu cầu experience riêng cho skill này
         * => không được phạt candidate.
         */
        if (requiredExperience == null || requiredExperience <= 0) {
            return 1.0;
        }

        Double candidateExperience = candidateSkill.getYearsOfExperience();

        /*
         * Job yêu cầu experience nhưng CV không có thông tin.
         *
         * Không nên coi null = đạt.
         */
        if (candidateExperience == null || candidateExperience <= 0) {
            return 0.0;
        }

        /*
         * Candidate đủ hoặc vượt yêu cầu
         */
        if (candidateExperience >= requiredExperience) {
            return 1.0;
        }

        /*
         * Candidate thiếu kinh nghiệm
         *
         * Ví dụ:
         * Job       = 5 năm
         * Candidate = 3 năm
         *
         * => 3 / 5 = 0.6
         */
        return Math.max(
                0.0,
                Math.min(
                        1.0,
                        candidateExperience / requiredExperience
                )
        );
    }

    private double calculateLevelScore(
            JobSkill jobSkill,
            CandidateSkill candidateSkill
    ) {

        SkillLevel requiredLevel = jobSkill.getLevel();

        /*
         * Job không yêu cầu level
         */
        if (requiredLevel == null) {
            return 1.0;
        }

        SkillLevel candidateLevel =
                candidateSkill.getLevel();

        /*
         * Job yêu cầu level nhưng CV không khai báo
         */
        if (candidateLevel == null) {
            return 0.0;
        }

        /*
         * Đạt hoặc vượt yêu cầu
         */
        if (candidateLevel.ordinal() >= requiredLevel.ordinal()) {
            return 1.0;
        }

        /*
         * Ví dụ:
         *
         * BEGINNER     = 0
         * INTERMEDIATE = 1
         * ADVANCED     = 2
         * EXPERT       = 3
         *
         * Required = EXPERT (3)
         * Candidate = INTERMEDIATE (1)
         *
         * => 1 / 3 = 0.33
         */
        int requiredOrdinal = requiredLevel.ordinal();
        int candidateOrdinal = candidateLevel.ordinal();

        if (requiredOrdinal <= 0) {
            return 0.0;
        }

        return Math.max(
                0.0,
                Math.min(
                        1.0,
                        (double) candidateOrdinal / requiredOrdinal
                )
        );
    }

    private double calculateConfidenceScore(
            CandidateSkill candidateSkill
    ) {

        /*
         * Nếu không phải AI matching,
         * coi mapping là đáng tin cậy.
         */
        if (candidateSkill.getSource() != SkillSource.MATCHED_BY_AI) {
            return 1.0;
        }

        Double confidence = candidateSkill.getConfidence();

        /*
         * AI match nhưng không có confidence
         * => không phạt.
         */
        if (confidence == null) {
            return 1.0;
        }

        return Math.max(
                0.0,
                Math.min(
                        1.0,
                        confidence
                )
        );
    }

    private double resolvePriority(JobSkill jobSkill) {

        Integer priority = jobSkill.getPriority();

        if (priority == null || priority <= 0) {
            return 1.0;
        }

        return priority.doubleValue();
    }

    private String formatExperience(Number value) {

        if (value == null) {
            return "không rõ";
        }

        return "%.1f".formatted(value.doubleValue());
    }


    // ==================== EXPERIENCE SCORE ====================

    private double scoreExperience(CandidateProfile candidateProfile, Integer experienceRequired) {
        double candidateYears = candidateProfile.getTotalExperienceYears() == null
                ? 0.0 : candidateProfile.getTotalExperienceYears();
        double requiredYears = experienceRequired == null ? 0 : experienceRequired;

        if (requiredYears <= 0) return 100.0;
        if (candidateYears >= requiredYears) return 100.0;
        return Math.max(0.0, 100.0 * candidateYears / requiredYears);
    }


    // ==================== EDUCATION / PROJECT SCORE (theo nhãn relevance AI gán) ====================

    private <T> double scoreByRelevance(List<T> items, Function<T, String> relevanceExtractor) {
        if (items == null || items.isEmpty()) {
            return NEUTRAL_WHEN_EMPTY;
        }
        double total = 0;
        for (T item : items) {
            total += switch (safe(relevanceExtractor.apply(item))) {
                case "RELEVANT" -> RELEVANT;
                case "PARTIAL" -> PARTIAL;
                default -> NOT_RELEVANT;
            };
        }
        return total / items.size();
    }

    private String safe(String s) {
        return s == null ? "" : s.toUpperCase();
    }

    private double round2(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    private record SkillScoringOutcome(double skillScore, List<SkillMatchOutcome> outcomes) {
    }
}


