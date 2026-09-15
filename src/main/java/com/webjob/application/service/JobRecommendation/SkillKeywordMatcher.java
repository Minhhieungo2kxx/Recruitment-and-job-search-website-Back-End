package com.webjob.application.service.JobRecommendation;

import com.webjob.application.dto.record.SkillEntry;
import com.webjob.application.dto.record.SkillMatch;
import com.webjob.application.dto.record.SkillSearchIndex;
import com.webjob.application.enums.SkillStatus;
import com.webjob.application.models.Entity.Skill;
import com.webjob.application.models.Entity.SkillAlias;
import com.webjob.application.repository.SkillAliasRepository;
import com.webjob.application.repository.SkillRepository;
import com.webjob.application.utils.common.UtilFormat;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ahocorasick.trie.Emit;
import org.ahocorasick.trie.Trie;
import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class SkillKeywordMatcher {
    private final SkillRepository skillRepository;
    private final SkillAliasRepository skillAliasRepository;


    private volatile SkillSearchIndex currentIndex;


    private static final Duration INDEX_TTL =
            Duration.ofMinutes(30);


    public List<String> extractKnownSkillNames(String rawCvText) {

        if (rawCvText == null || rawCvText.isBlank()) {
            return List.of();
        }

        ensureIndexLoaded();
        SkillSearchIndex index = this.currentIndex;
        if (index == null || index.trie() == null || index.skillIndex().isEmpty()) {
            return List.of();
        }



        String normalizedCv = UtilFormat.normalize(rawCvText);

        if (normalizedCv.isEmpty()) {
            return List.of();
        }


        Collection<Emit> emits = index.trie().parseText(normalizedCv);
        if (emits.isEmpty()) {
            return List.of();
        }

        List<SkillMatch> validMatches = new ArrayList<>();

        for (Emit emit : emits) {

            String normalizedKeyword = emit.getKeyword();

            if (!containsWholeSkill(
                    normalizedCv,
                    emit.getStart(),
                    emit.getEnd()
            )) {
                continue;
            }


            Skill skill = index.skillIndex().get(normalizedKeyword);

            if (skill == null) {
                continue;
            }

            validMatches.add(
                    new SkillMatch(
                            normalizedKeyword,
                            skill,
                            emit.getStart(),
                            emit.getEnd()
                    )
            );
        }

        if (validMatches.isEmpty()) {
            return List.of();
        }

        List<SkillMatch> selectedMatches = selectNonOverlappingMatches(validMatches);


        Map<Long, Skill> result = new LinkedHashMap<>();

        for (SkillMatch match : selectedMatches) {

            result.putIfAbsent(
                    match.skill().getId(),
                    match.skill()
            );
        }

        return result.values()
                .stream()
                .map(Skill::getName)
                .toList();
    }


private void ensureIndexLoaded() {

    SkillSearchIndex index = this.currentIndex;

    if (!isIndexValid(index)) {
        synchronized (this) {
            /*
             * Double-check sau khi lấy lock.
             */
            index = this.currentIndex;

            if (!isIndexValid(index)) {
                reload();
            }
        }
    }
}



private boolean isIndexValid(SkillSearchIndex index) {

    if (index == null || index.trie() == null) {

        return false;
    }

    Instant loadedAt = index.loadedAt();

    if (loadedAt == null) {
        return false;
    }

    return Duration
            .between(loadedAt, Instant.now())
            .compareTo(INDEX_TTL) <= 0;
}


    private void reload() {

        long start = System.currentTimeMillis();

        List<Skill> activeSkills = skillRepository.findAllByStatus(SkillStatus.ACTIVE);


        List<SkillAlias> activeAliases = skillAliasRepository.findAllActiveAliases(
                        SkillStatus.ACTIVE,
                        SkillStatus.ACTIVE
                );

        Map<String, Skill> newIndex = new LinkedHashMap<>();

        /*
         * Add canonical Skill name trước.
         */
        for (Skill skill : activeSkills) {

            String normalizedName = UtilFormat.normalize(skill.getName());

            if (normalizedName.isEmpty()) {
                continue;
            }

            newIndex.putIfAbsent(normalizedName, skill);
        }

//        Add Skill aliases
        for (SkillAlias alias : activeAliases) {

            Skill skill = alias.getSkill();

            if (skill == null) {
                continue;
            }

            String normalizedAlias = alias.getNormalizedAlias();


            if (normalizedAlias == null || normalizedAlias.isBlank()) {
                continue;
            }

            newIndex.putIfAbsent(normalizedAlias, skill);
        }


        Trie.TrieBuilder builder = Trie.builder();

        for (String keyword : newIndex.keySet()) {
            builder.addKeyword(keyword);
        }

        Trie newTrie = builder.build();
        SkillSearchIndex newSearchIndex =
                new SkillSearchIndex(
                        newTrie,
                        newIndex,
                        Instant.now()
                );

        this.currentIndex = newSearchIndex;




        long elapsed = System.currentTimeMillis() - start;

        log.debug(
                "SkillKeywordMatcher: reloaded {} skills, {} aliases in {} ms",
                activeSkills.size(),
                activeAliases.size(),
                elapsed
        );
    }





    private boolean containsWholeSkill(
            String text,
            int start,
            int end
    ) {

        char firstChar = text.charAt(start);

        char lastChar = text.charAt(end);

        if (start > 0) {

            char previous = text.charAt(start - 1);

            if (isWordChar(previous) && isWordChar(firstChar)) {

                return false;
            }
        }

        if (end < text.length() - 1) {

            char next = text.charAt(end + 1);

            if (isWordChar(next) && isWordChar(lastChar)) {

                return false;
            }
        }

        return true;
    }


    private boolean isWordChar(char c) {

        return Character.isLetterOrDigit(c);
    }


    private List<SkillMatch> selectNonOverlappingMatches(
            List<SkillMatch> matches
    ) {

        List<SkillMatch> sorted =
                matches.stream()
                        .sorted(
                                Comparator
                                        .comparingInt(
                                                SkillMatch::start
                                        )
                                        .thenComparing(
                                                Comparator
                                                        .comparingInt(
                                                                SkillMatch::length
                                                        )
                                                        .reversed()
                                        )
                        )
                        .toList();

        List<SkillMatch> selected =
                new ArrayList<>();

        int lastSelectedEnd = -1;

        for (SkillMatch current : sorted) {

            if (current.start() > lastSelectedEnd) {

                selected.add(current);

                lastSelectedEnd =
                        current.end();
            }
        }

        return selected;


    }
}
