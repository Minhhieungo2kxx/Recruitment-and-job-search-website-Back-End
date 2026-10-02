package com.webjob.application.service.JobMatchingHR.Ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.webjob.application.component.LLM.LLMGeminiClient;
import com.webjob.application.dto.Request.ParsedSkill;
import com.webjob.application.models.Entity.Skill;
import com.webjob.application.repository.SkillRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Chỉ được gọi cho phần skill KHÔNG khớp được qua SkillAlias (rule-based) -
 * số lượng skill lạ mỗi CV thường rất nhỏ (2-5 skill), nên chi phí AI call ở
 * bước này không đáng kể so với lợi ích chuẩn hoá đúng.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SkillSemanticMatcher {

    private final SkillRepository skillRepository;

    private final LLMGeminiClient llmGeminiClient;



    private static final String SYSTEM_INSTRUCTION = """
            Bạn là AI chuẩn hóa tên kỹ năng (skill) đa ngành cho hệ thống tuyển dụng.

            NHIỆM VỤ:
            Với mỗi rawText, chọn skill phù hợp nhất từ candidateSkillList dựa trên:
            - tên đầy đủ/viết tắt
            - lỗi chính tả, biến thể tên
            - tên gọi phổ biến, alias
            - cách viết khác nhưng cùng công nghệ/kỹ năng

            QUY TẮC:
            1. Chỉ được chọn matchedSkillId có trong candidateSkillList.
            2. Chỉ match khi rawText và candidate thực sự chỉ cùng một skill/khái niệm.
            3. Không suy diễn từ quan hệ gần, cùng lĩnh vực, hoặc kỹ năng thường đi kèm.
            4. Nếu không có candidate phù hợp, trả matchedSkillId=null và confidence=0.
            5. confidence nằm trong [0,1], phản ánh độ chắc chắn của việc match.
            6. Ưu tiên độ chính xác hơn việc cố gắng tìm một kết quả.
            7. Chỉ trả dữ liệu đúng response schema; không giải thích thêm.
            """;



    private static final Map<String, Object> RESPONSE_SCHEMA = Map.of(
            "type", "OBJECT",
            "properties", Map.of(
                    "matches", Map.of("type", "ARRAY", "items", Map.of(
                            "type", "OBJECT",
                            "properties", Map.of(
                                    "rawText", Map.of("type", "STRING"),
                                    "matchedSkillId", Map.of("type", "INTEGER"),
                                    "confidence", Map.of("type", "NUMBER")
                            ),
                            "required", List.of("rawText", "confidence")
                    ))
            ),
            "required", List.of("matches")
    );


    public Map<String, Match> match(List<ParsedSkill> unmatchedTexts) {
        if (unmatchedTexts == null || unmatchedTexts.isEmpty()) {
            log.debug("unmatchedTexts is empty, skipping AI skill matching.");
            return Map.of();
        }

        List<Skill> candidateSkillList = skillRepository.findAll();
        if (candidateSkillList.isEmpty()) {
            log.warn("No candidate skills found in database. Skipping skill matching.");
            return Map.of();
        }


        log.info("Starting AI skill matching. unmatchedTextsCount={}, availableSkillsCount={}",
                unmatchedTexts.size(), candidateSkillList.size());

        List<String> unmatchedTextNames=unmatchedTexts.stream()
                .map(ParsedSkill::getName)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        String userContent = buildUserContent(unmatchedTextNames, candidateSkillList);
        JsonNode json = llmGeminiClient.callWithFallback(SYSTEM_INSTRUCTION, userContent, RESPONSE_SCHEMA).json();

        Map<Long, Skill> skillById = candidateSkillList.stream()
                .collect(Collectors.toMap(Skill::getId, Function.identity()));

        Map<String, Match> result = new HashMap<>();
        json.path("matches").forEach(node -> {
            String rawText = node.path("rawText").asText();
            double confidence = node.path("confidence").asDouble(0);
            Long skillId = node.hasNonNull("matchedSkillId") ? node.path("matchedSkillId").asLong() : null;
            Skill skill = skillId != null ? skillById.get(skillId) : null;
            result.put(rawText, new Match(skill, confidence));
        });


        log.info("Successfully completed AI skill matching. requestedCount={}, mappedCount={}",
                unmatchedTexts.size(), result.size());

        return result;
    }

    private String buildUserContent(List<String> unmatchedTexts, List<Skill> candidateSkillList) {
        StringBuilder sb = new StringBuilder();
        sb.append("rawTexts cần chuẩn hoá:\n");
        unmatchedTexts.forEach(t -> sb.append("- ").append(t).append("\n"));

        sb.append("\ncandidateSkillList (id: tên):\n");
        candidateSkillList.forEach(s -> sb.append(s.getId()).append(": ").append(s.getName()).append("\n"));

        return sb.toString();
    }

    /**
     *  AI để suy luận skill semantic (vd "Postgre" -> "PostgreSQL").
     * trả về { "<rawText>": { "skillId": ..., "confidence": ... } }.
     */
    public record Match(Skill skill, double confidence) {
    }
}


