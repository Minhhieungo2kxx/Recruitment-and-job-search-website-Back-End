package com.webjob.application.dto.record;

import com.webjob.application.models.Entity.Skill;
import org.ahocorasick.trie.Trie;

import java.time.Instant;
import java.util.Map;

public record SkillSearchIndex(
        Trie trie,
        Map<String, Skill> skillIndex,
        Instant loadedAt
) {
    public SkillSearchIndex {
        skillIndex = Map.copyOf(skillIndex);
    }
}
