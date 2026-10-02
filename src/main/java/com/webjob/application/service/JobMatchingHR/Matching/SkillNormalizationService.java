package com.webjob.application.service.JobMatchingHR.Matching;


import com.webjob.application.dto.Request.ParsedSkill;
import com.webjob.application.dto.record.*;
import com.webjob.application.enums.SkillSource;
import com.webjob.application.enums.SkillStatus;
import com.webjob.application.models.Entity.CandidateProfile;
import com.webjob.application.models.Entity.CandidateSkill;
import com.webjob.application.models.Entity.Skill;
import com.webjob.application.models.Entity.SkillAlias;
import com.webjob.application.repository.SkillAliasRepository;
import com.webjob.application.repository.SkillRepository;
import com.webjob.application.service.JobMatchingHR.Ai.SkillSemanticMatcher;
import com.webjob.application.utils.common.UtilFormat;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ahocorasick.trie.Emit;
import org.ahocorasick.trie.Trie;
import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.time.Instant;
import java.util.*;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Chuẩn hoá danh sách skill text thô (AI extract từ CV) về Skill chuẩn của hệ thống.
 * Bước 1 (rule-based, tức thì, miễn phí): so khớp qua SkillAlias.normalizedAlias.
 * Bước 2 (AI semantic, chỉ gọi cho phần chưa khớp): xem AiSkillSemanticMatcher -
 * tách interface riêng để không phụ thuộc cứng vào Gemini, dễ mock khi test.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SkillNormalizationService {

    private final SkillSemanticMatcher aiSkillSemanticMatcher; // gọi AI, xem interface bên dưới

    private final SkillRepository skillRepository;
    private final SkillAliasRepository skillAliasRepository;




    private volatile SkillSearchIndexMatching currentIndex;
    private static final Pattern NON_ALNUM = Pattern.compile("[^a-z0-9]");


//    /**
//     * @param rawSkillTexts danh sách text skill thô do AI extract từ CV (vd ["ReactJS", "Node", "Docker"])
//     */
    public List<CandidateSkill> normalize(
            CandidateProfile profile,
            List<ParsedSkill> parsedSkills
    ) {
        if (parsedSkills == null || parsedSkills.isEmpty()) {
            return List.of();
        }
        SkillSearchIndexMatching index = currentIndex;
        if (index == null) {
            reload();
            index = currentIndex;
        }
        if (index == null) {
            log.warn("SkillKeywordMatcher: unable to initialize skill index");
            return List.of();
        }
        Map<Long, CandidateSkill> matchedSkills = new LinkedHashMap<>();

        List<ParsedSkill> unmatchedTexts = new ArrayList<>();

        for (ParsedSkill raw : parsedSkills) {
            if (raw.getName() == null || raw.getName().isBlank()) {
                continue;
            }
            SkillMatchingCV match = findBestMatch(index, raw.getName());

            if (match == null) {
                unmatchedTexts.add(raw);
                continue;
            }

            Skill skill = match.skill();

            if (skill == null || skill.getId() == null) {
                continue;
            }

            CandidateSkill candidateSkill =
                    CandidateSkill.builder()
                            .candidateProfile(profile)
                            .skill(skill)
                            .rawText(raw.getName())
                            .level(raw.getLevel())
                            .yearsOfExperience(raw.getYearsOfExperience())
                            .source(match.source())
                            .confidence(match.confidence())
                            .build();

            /*
             * Nếu cùng một Skill được match nhiều lần,
             * giữ kết quả có confidence cao hơn.
             */
            matchedSkills.merge(
                    skill.getId(),
                    candidateSkill,
                    this::keepHigherConfidence
            );
        }
        /*
         * Chỉ dùng AI cho những skill mà
         * local matcher hoàn toàn không tìm thấy.
         */
        if (!unmatchedTexts.isEmpty()) {

            List<CandidateSkill> aiResults = resolveWithAi(profile, unmatchedTexts);
            if (aiResults != null) {
                for (CandidateSkill candidateSkill : aiResults) {

                    if (candidateSkill == null || candidateSkill.getSkill() == null
                            || candidateSkill.getSkill().getId() == null) {
                        continue;
                    }
                    matchedSkills.merge(
                            candidateSkill.getSkill().getId(),
                            candidateSkill,
                            this::keepHigherConfidence
                    );
                }
            }
        }
        return new ArrayList<>(matchedSkills.values());
    }
    private CandidateSkill keepHigherConfidence(
            CandidateSkill current,
            CandidateSkill incoming
    ) {
        if (current == null) {
            return incoming;
        }

        if (incoming == null) {
            return current;
        }

        return incoming.getConfidence() > current.getConfidence()
                ? incoming
                : current;
    }
    private SkillMatchingCV findBestMatch(SkillSearchIndexMatching index, String raw) {

        String normalized = UtilFormat.normalize(raw);
        if (normalized.isBlank()) {
            return null;
        }
        /*
         * 1. EXACT MATCH
         */
        SkillKeyword exact = index.keywordIndex().get(normalized);

        if (exact != null) {

            return new SkillMatchingCV(
                    exact.skill(),
                    exact.source(),
                    1.0,
                    normalized
            );
        }

        /*
         * 2. AHO-CORASICK
         */
        Collection<Emit> emits = index.trie().parseText(normalized);
        if (emits.isEmpty()) {
            return null;
        }
        List<Emit> validMatches = emits.stream().filter(e ->
                                isValidBoundary(
                                        normalized,
                                        e.getStart(),
                                        e.getEnd()
                                )
                        )
                        .toList();

        if (validMatches.isEmpty()) {
            return null;
        }

        Emit best = validMatches.stream()
                        .max(
                                Comparator.comparingInt(
                                                (Emit e) ->
                                                        e.getEnd() - e.getStart() + 1
                                        )
                                        .thenComparingInt(
                                                Emit::getStart
                                        )
                        )
                        .orElse(null);

        if (best == null) {
            return null;
        }

        SkillKeyword matched = index.keywordIndex().get(best.getKeyword());

        if (matched == null) {
            return null;
        }
        return new SkillMatchingCV(
                matched.skill(),
                matched.source(),
                calculateConfidence(
                        normalized,
                        best.getKeyword()
                ),
                best.getKeyword()
        );
    }


    private double calculateConfidence(
            String normalizedRaw,
            String keyword
    ) {

        if (normalizedRaw.equals(keyword)) {
            return 1.0;
        }
        return 0.95;
    }


    private List<CandidateSkill> resolveWithAi(CandidateProfile profile, List<ParsedSkill> unmatchedTexts) {

        Map<String, SkillSemanticMatcher.Match> aiMatches = aiSkillSemanticMatcher.match(unmatchedTexts);

        return unmatchedTexts.stream().map(raw -> {
            SkillSemanticMatcher.Match match = aiMatches.get(raw.getName());
            if (match == null || match.skill() == null || match.confidence() < 0.6) {
                return CandidateSkill.builder()
                        .candidateProfile(profile)
                        .rawText(raw.getName())
                        .level(raw.getLevel())
                        .yearsOfExperience(raw.getYearsOfExperience())
                        .source(SkillSource.UNMATCHED)
                        .build();
            }
            return CandidateSkill.builder()
                    .candidateProfile(profile)
                    .skill(match.skill())
                    .rawText(raw.getName())
                    .level(raw.getLevel())
                    .yearsOfExperience(raw.getYearsOfExperience())
                    .source(SkillSource.MATCHED_BY_AI)
                    .confidence(match.confidence())
                    .build();
        }).toList();
    }

    /**
     * Bỏ dấu, lowercase, bỏ ký tự đặc biệt - "Node.js" và "nodejs" phải cùng 1 key
     */
    private String normalizeText(String text) {
        if (text == null) return "";
        String noAccent = Normalizer.normalize(text, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        return NON_ALNUM.matcher(noAccent.toLowerCase()).replaceAll("");
    }

    private void reload() {

        long start = System.currentTimeMillis();

        List<Skill> activeSkills = skillRepository.findAllByStatus(SkillStatus.ACTIVE);
        List<SkillAlias> activeAliases = skillAliasRepository.findAllActiveAliases(
                        SkillStatus.ACTIVE,
                        SkillStatus.ACTIVE
                );

        Map<String, SkillKeyword> newIndex = new LinkedHashMap<>();
        /*
         * 1. Add canonical skill names first.
         *
         * Canonical name luôn được ưu tiên hơn alias.
         */
        for (Skill skill : activeSkills) {
            if (skill == null || skill.getId() == null || skill.getName() == null) {
                continue;
            }
            String normalizedName = UtilFormat.normalize(skill.getName());
            if (normalizedName.isBlank()) {
                continue;
            }
            newIndex.putIfAbsent(
                    normalizedName,
                    new SkillKeyword(skill, SkillSource.MATCHED_BY_NAME)
            );
        }
        /*
         * 2. Add aliases.
         */
        for (SkillAlias alias : activeAliases) {

            if (alias == null || alias.getSkill() == null || alias.getSkill().getId() == null) {
                continue;
            }

            String normalizedAlias = UtilFormat.normalize(alias.getNormalizedAlias());

            if (normalizedAlias.isBlank()) {
                continue;
            }

            SkillKeyword existing =
                    newIndex.putIfAbsent(
                            normalizedAlias,
                            new SkillKeyword(alias.getSkill(), SkillSource.MATCHED_BY_ALIAS)
                    );

        }
        /*
         * 3. Build Aho-Corasick Trie.
         */
        Trie.TrieBuilder builder = Trie.builder();

        for (String keyword : newIndex.keySet()) {
            builder.addKeyword(keyword);
        }

        Trie newTrie = builder.build();

        /*
         * 4. Immutable snapshot.
         */
        SkillSearchIndexMatching newSearchIndex =
                new SkillSearchIndexMatching(
                        newTrie,
                        Map.copyOf(newIndex),
                        Instant.now()
                );

        /*
         * 5. Atomic swap.
         *
         * Các request đang dùng index cũ
         * vẫn tiếp tục dùng index cũ.
         *
         * Request mới sẽ lấy index mới.
         */
        this.currentIndex = newSearchIndex;
        long elapsed = System.currentTimeMillis() - start;

        log.debug(
                "SkillKeywordMatcher: reloaded {} skills, {} aliases, {} keywords in {} ms",
                activeSkills.size(),
                activeAliases.size(),
                newIndex.size(),
                elapsed
        );
    }
    private boolean isValidBoundary(
            String text,
            int start,
            int end
    ) {

        if (start > 0) {
            char before = text.charAt(start - 1);
            if (isWordChar(before)) {
                return false;
            }
        }

        // 2. Kiểm tra ký tự đứng SAU từ khớp
        if (end + 1 < text.length()) {
            char after = text.charAt(end + 1);
            if (isWordChar(after)) {
                return false; // Dính liền với một chữ cái/số ở phía sau -> Không hợp lệ
            }
        }

        return true; // Thỏa mãn là một từ độc lập
    }

    private boolean isWordChar(char c) {

        return Character.isLetterOrDigit(c)
                || c == '_';
    }



}



