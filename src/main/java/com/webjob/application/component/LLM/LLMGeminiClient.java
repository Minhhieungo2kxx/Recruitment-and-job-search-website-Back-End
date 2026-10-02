package com.webjob.application.component.LLM;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.webjob.application.exception.Customs.GeminiUnavailableException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

@Component
@RequiredArgsConstructor
@Slf4j
public class LLMGeminiClient {
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${gemini.api.key}")
    private String apiKeysRaw;

    @Value("${gemini.api.base-url}")
    private String baseUrl;

    @Value("${gemini.api.models}")
    private String modelsRaw;

    private final AtomicInteger keyRotationIndex = new AtomicInteger(0);


    /** Số lần thử lại TRÊN CÙNG 1 key/model khi gặp lỗi tạm thời (JSON hỏng, mạng chập chờn)
     * trước khi chuyển sang key khác - tránh đốt hết key chỉ vì 1 lần lỗi thoáng qua. */
    private static final int MAX_ATTEMPTS_PER_KEY = 2;
    private static final long BASE_BACKOFF_MS = 400;


    /**
     * @param systemInstruction  prompt hệ thống (vai trò + quy tắc)
     * @param userContent        nội dung cần AI xử lý (vd rawText CV)
     * @param responseSchema     Gemini responseSchema (Map) để ép JSON output đúng cấu trúc
     */
    public LLMGeminiResult callWithFallback(String systemInstruction, String userContent, Map<String, Object> responseSchema) {
        List<Map<String, Object>> contents = List.of(
                Map.of("role", "user", "parts", List.of(Map.of("text", userContent)))
        );

        List<String> models = Arrays.asList(modelsRaw.split(","));
        List<String> keys = Arrays.asList(apiKeysRaw.split(","));

        Exception lastException = null;

        for (String model : models) {
            boolean modelNotFound = false;

            for (int i = 0; i < keys.size(); i++) {
                String apiKey = nextKey(keys);
                AttemptOutcome outcome = tryWithRetry(model.trim(), apiKey, systemInstruction, contents, responseSchema);

                if (outcome.result() != null) {
                    return outcome.result();
                }

                lastException = outcome.failure();
                if (outcome.failure() instanceof HttpClientErrorException httpEx) {
                    int status = httpEx.getStatusCode().value();
                    if (status == 404) {
                        log.warn("Model {} not found -> next model", model);
                        modelNotFound = true;
                        break;
                    }
                    if (status != 429 && status != 403) {
                        throw httpEx; // lỗi client thật sự (400, 401...) - không nên rotate mù quáng
                    }
                    log.warn("Key {} failed ({}) -> next key", maskKey(apiKey), status);
                }

            }

            if (modelNotFound) {
                continue;
            }
        }
        throw new GeminiUnavailableException("Tất cả model/key Gemini đều không khả dụng", lastException);
    }

    /**
     * Thử tối đa MAX_ATTEMPTS_PER_KEY lần trên CÙNG 1 key/model với backoff tăng dần,
     * chỉ retry cho lỗi TẠM THỜI (JSON hỏng, timeout mạng). Lỗi 4xx do sai request
     * (400/401...) trả ngay để callWithFallback throw, không lãng phí retry.
     *
     * Trả về AttemptOutcome thay vì dùng field instance để giữ method THREAD-SAFE -
     * bean này là singleton, có thể bị gọi đồng thời từ nhiều thread khi chạy matching
     * song song (@Async + thread pool) cho nhiều candidate cùng lúc.
     */
    private AttemptOutcome tryWithRetry(
            String model, String apiKey, String systemInstruction,
            List<Map<String, Object>> contents, Map<String, Object> responseSchema
    ) {
        Exception lastFailure = null;

        for (int attempt = 1; attempt <= MAX_ATTEMPTS_PER_KEY; attempt++) {
            try {
                log.info("Gemini matching call: model={} key={} attempt={}", model, maskKey(apiKey), attempt);
                JsonNode json = callGeminiRaw(model, apiKey, systemInstruction, contents, responseSchema);
                return new AttemptOutcome(new LLMGeminiResult(json, model), null);

            } catch (HttpClientErrorException e) {
                return new AttemptOutcome(null, e);

            } catch (GeminiUnavailableException e) {
                lastFailure = e;
                if (attempt < MAX_ATTEMPTS_PER_KEY) {
                    long backoff = BASE_BACKOFF_MS * attempt;
                    log.warn("Gemini response không hợp lệ ({}), model={} key={} - retry sau {}ms",
                            e.getMessage(), model, maskKey(apiKey), backoff);
                    sleep(backoff);
                } else {
                    log.warn("Gemini response không hợp lệ sau {} lần thử, model={} key={} -> thử key khác",
                            MAX_ATTEMPTS_PER_KEY, model, maskKey(apiKey));
                }
            }
        }
        return new AttemptOutcome(null, lastFailure);
    }

    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
    }


    private JsonNode callGeminiRaw(String model, String apiKey,
                                   String systemInstruction,
                                   List<Map<String, Object>> contents,
                                   Map<String, Object> responseSchema) {
        try {
            Map<String, Object> body = new HashMap<>();
            body.put("systemInstruction", Map.of("parts", List.of(Map.of("text", systemInstruction))));
            body.put("contents", contents);

            if (responseSchema != null) {
                body.put("generationConfig", Map.of(
                        "responseMimeType", "application/json",
                        "responseSchema", responseSchema
                ));
            }

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("X-goog-api-key", apiKey);

            String url = baseUrl + "/" + model + ":generateContent";
            ResponseEntity<String> response = restTemplate.postForEntity(url, new HttpEntity<>(body, headers), String.class);

            return extractJsonText(objectMapper.readTree(response.getBody()));

        } catch (HttpClientErrorException e) {
            throw e;
        } catch (JsonProcessingException e) {
            throw new GeminiUnavailableException("Gemini trả về JSON không hợp lệ", e);
        } catch (RestClientException e) {
            throw new GeminiUnavailableException("Không thể gọi Gemini API", e);
        }
    }


    private JsonNode extractJsonText(JsonNode geminiResponse) {
        JsonNode candidates = geminiResponse.path("candidates");
        if (!candidates.isArray() || candidates.isEmpty()) {
            String blockReason = geminiResponse.path("promptFeedback").path("blockReason").asText(null);
            throw new GeminiUnavailableException(
                    blockReason != null ? "Gemini từ chối xử lý request: " + blockReason : "Gemini không trả về candidates nào",
                    null
            );
        }

        JsonNode first = candidates.get(0);
        String finishReason = first.path("finishReason").asText("");
        if ("SAFETY".equals(finishReason) || "RECITATION".equals(finishReason)) {
            throw new GeminiUnavailableException("Gemini chặn nội dung do finishReason=" + finishReason, null);
        }

        String rawText = first.path("content").path("parts").path(0).path("text").asText(null);
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



    private String nextKey(List<String> keys) {
        int idx = Math.floorMod(keyRotationIndex.getAndIncrement(), keys.size());
        return keys.get(idx).trim();
    }

    private String maskKey(String key) {
        if (key == null || key.length() < 8) return "****";
        return key.substring(0, 4) + "..." + key.substring(key.length() - 4);
    }

    /** Kết quả gọi Gemini kèm tên model THỰC TẾ đã trả lời thành công */
    public record LLMGeminiResult(JsonNode json, String model) {
    }

    /** Kết quả 1 lượt thử (thành công hoặc lỗi cuối cùng) */
    private record AttemptOutcome(LLMGeminiResult result, Exception failure) {
    }

}
