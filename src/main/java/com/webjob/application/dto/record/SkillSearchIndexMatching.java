package com.webjob.application.dto.record;

import org.ahocorasick.trie.Trie;

import java.time.Instant;
import java.util.Map;

public record SkillSearchIndexMatching(
        Trie trie,
        Map<String, SkillKeyword> keywordIndex,
        Instant loadedAt
) {
}
