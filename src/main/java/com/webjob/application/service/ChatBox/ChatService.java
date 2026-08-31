package com.webjob.application.service.ChatBox;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.webjob.application.dto.Request.Chatbox.ChatMessageDto;
import com.webjob.application.dto.Response.ApiResponse;
import com.webjob.application.dto.Response.LoopOutcome;
import com.webjob.application.enums.OutcomeType;
import com.webjob.application.exception.Customs.ChatHistoryException;
import com.webjob.application.exception.Customs.ChatProcessingException;
import com.webjob.application.exception.Customs.GeminiUnavailableException;
import com.webjob.application.models.Entity.ChatMessage;
import com.webjob.application.models.Entity.User;
import com.webjob.application.repository.ChatMessageRepository;
import com.webjob.application.service.UserService;
import com.webjob.application.utils.common.SecurityUtils;
import com.webjob.application.utils.common.UtilFormat;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChatService {

    @Value("${gemini.api.key}")
    private String apiKeys;

    @Value("${gemini.api.base-url}")
    private String baseUrl;
    @Value("${gemini.api.models}")
    private String models;
    private final UserService userService;
    private final ChatMessageRepository chatMessageRepository;

    private final SecurityUtils securityUtils;

    private final ToolDefinitions toolDefinitions;

    private final ChatToolService chatToolService;

    private final ObjectMapper objectMapper;
    private final AtomicInteger keyIndex = new AtomicInteger(0);

    private static final int MAX_FUNCTION_CALL_ROUNDS = 5;

    private static final String SYSTEM_TEMPLATE = """
        Bạn là trợ lý tư vấn tuyển dụng và việc làm của một website việc làm.

        ========================
        VAI TRÒ VÀ PHẠM VI
        ========================

        NHIỆM VỤ:
        - Chỉ trả lời các câu hỏi liên quan đến tuyển dụng, tìm việc,
          nghề nghiệp, CV, phỏng vấn và định hướng nghề nghiệp.
        - Không trả lời các câu hỏi ngoài phạm vi việc làm.

        Khi cần thông tin thật từ hệ thống:
        - Danh sách công việc.
        - Chi tiết công việc.
        - Thông tin công ty.
        - Lịch sử ứng tuyển.
        - Gợi ý việc làm.
        - Lưu yêu thích.
        - Xóa yêu thích.

        BẮT BUỘC:
        - Phải gọi tool tương ứng.
        - Không tự suy đoán dữ liệu.
        - Không tự tạo ID.


        ========================
        QUY TẮC SỬ DỤNG ID
        ========================

        Hệ thống sử dụng marker ID để ghi nhớ các đối tượng trong lịch sử hội thoại.

        Khi trả lời có chứa dữ liệu từ tool, BẮT BUỘC giữ ID theo format:

        Công việc:
        [JOB_ID:<id>]

        Công ty:
        [COMPANY_ID:<id>]


        Ví dụ danh sách việc làm:

        1. [JOB_ID:123] Java Backend Developer
           Công ty: ABC Technology
           Lương: 30 - 40 triệu

        2. [JOB_ID:456] Python Backend Engineer
           Công ty: XYZ Company
           Lương: 25 - 35 triệu


        Ví dụ thông tin công ty:

        [COMPANY_ID:20] Công ty ABC Technology

        QUY ĐỊNH:
        - Không được bỏ marker ID.
        - Không thay đổi tên marker.
        - Không tạo marker cho ID không tồn tại.
        - Chỉ sử dụng ID được trả về từ tool hoặc có trong lịch sử hội thoại.


        ========================
        QUY TẮC THAM CHIẾU LỊCH SỬ
        ========================

        Ví dụ khi user nói:

        - "job đầu tiên"
        - "công việc thứ hai"
        - "job ở trên"
        - "công việc vừa tìm"
        - "lưu job này"
        - "xóa job này khỏi yêu thích"
        - "xem công ty của job này"

        Hãy tìm marker ID tương ứng trong lịch sử hội thoại.

        Ví dụ:

        Lịch sử:
        [JOB_ID:123] Java Backend Developer

        User:
        "Lưu công việc này"

        Phải gọi:

        saveFavoriteJob(jobId=123)


        ========================
        QUY TẮC FUNCTION
        ========================

        getJobDetail:
        - Chỉ gọi khi user muốn xem chi tiết một công việc cụ thể.
        - Bắt buộc phải có jobId.

        getCompany:
        - Chỉ gọi khi user muốn xem thông tin công ty.
        - Bắt buộc phải có companyId.

        saveFavoriteJob:
        - Chỉ gọi khi user muốn lưu một công việc yêu thích.
        - Bắt buộc phải có jobId.

        removeFavoriteJob:
        - Chỉ gọi khi user muốn xóa một công việc khỏi danh sách yêu thích.
        - Bắt buộc phải có jobId.


        ========================
        XỬ LÝ TOOL RESULT
        ========================

        Nếu tool trả về danh sách rỗng:
        - Nói rõ không tìm thấy.
        - Không tự bổ sung dữ liệu.


        ========================
        ĐỊNH DẠNG RESPONSE CUỐI
        ========================

        Nếu câu hỏi không liên quan việc làm:

        Chỉ trả về:

        [INVALID]


        Nếu câu hỏi hợp lệ:

        Chỉ trả về:

        [OK]
        <nội dung trả lời>


        QUY ĐỊNH:
        - Không thêm nội dung trước [OK] hoặc [INVALID].
        - Không giải thích về tool.
        - Không nhắc đến hệ thống.
        - Không nói về marker ID.
        - Luôn trả lời bằng tiếng Việt.
        - Trả lời ngắn gọn, chuyên nghiệp.
        """;


    public ChatMessageDto processMessage(ChatMessageDto messageDto, Authentication authentication) {
        try {
            User user = securityUtils.getCurrentUser();
            Long userId = user.getId();
            log.info("Processing message for user: {}", userId);
            List<ChatMessageDto> history = getChatHistoryLLMGemini(authentication);

            List<String> modelList = Arrays.stream(models.split(",")).map(String::trim).toList();
            List<String> keyList = Arrays.stream(apiKeys.split(",")).map(String::trim).toList();

            List<Map<String, Object>> initialContents = buildContents(messageDto.getMessage(), history);

            LoopOutcome outcome = runWithFallback(modelList, keyList, initialContents, userId, messageDto.getMessage());

            return finalizeOutcome(outcome, user, messageDto.getMessage());

        } catch (Exception e) {
            log.error("Error processing chat message", e);
            throw new ChatProcessingException("Đã có lỗi xảy ra khi xử lý tin nhắn: " ,e);
        }
    }


    // Lưu tin nhắn (theo user nếu có, ngược lại thì theo sessionId)
    public void saveMessage(User user, String message, String response) {
        ChatMessage chat = new ChatMessage();
        chat.setUser(user); // Gán entity User ở đây
        chat.setMessage(message);
        chat.setResponse(response);
        chatMessageRepository.save(chat);
    }

    // Lấy lịch sử chat, ưu tiên theo user nếu có
    public List<ChatMessageDto> getChatHistory(Authentication authentication) {
        try {
            List<ChatMessage> messages = chatMessageRepository.findByUserIdOrderByCreatedAtAsc(securityUtils.getCurrentUserId());

            return messages.stream()
                    .map(m -> ChatMessageDto.builder()
                            .message(m.getMessage())
                            .response(UtilFormat.formatForUser(m.getResponse()))
                            .timestamp(m.getCreatedAt())
                            .build())
                    .collect(Collectors.toList());

        } catch (Exception e) {
            throw new ChatHistoryException("Không thể lấy lịch sử chat: ", e);
        }

    }

    public List<ChatMessageDto> getChatHistoryLLMGemini(Authentication authentication) {
        try {
            List<ChatMessage> messages = chatMessageRepository.findByUserIdOrderByCreatedAtAsc(securityUtils.getCurrentUserId());

            return messages.stream()
                    .map(m -> ChatMessageDto.builder()
                            .message(m.getMessage())
                            .response(m.getResponse())
                            .timestamp(m.getCreatedAt())
                            .build())
                    .collect(Collectors.toList());

        } catch (Exception e) {
            throw new ChatHistoryException("Không thể lấy lịch sử chat: ", e);
        }

    }

    public void clearChatHistory(Authentication authentication) {
        try {
            User user = userService.getById(Long.valueOf(authentication.getName()));
            // Xóa trong DB theo user
            chatMessageRepository.deleteByUser(user);
            log.info("Cleared chat history for user: {}", user.getEmail());
        } catch (RuntimeException e) {
            throw new ChatHistoryException("Lỗi khi xóa lịch sử chat :", e);
        }


    }


    private boolean isInvalidResponse(String aiResponse) {
        return aiResponse != null && aiResponse.trim().equals("[INVALID]");
    }

    @Transactional
    public ChatMessageDto sendMessage(ChatMessageDto messageDto, Authentication authentication) {
        ChatMessageDto result = processMessage(messageDto, authentication);
        return result;
    }

    public List<ChatMessageDto> ListChatHistory(Authentication authentication) {
        List<ChatMessageDto> history = getChatHistory(authentication);
        return history;
    }


    @Transactional
    public void deleteChatHistory(Authentication authentication) {
        clearChatHistory(authentication);
    }

    private String getNextApiKey(List<String> keys) {

        int index = Math.abs(
                keyIndex.getAndIncrement()
        ) % keys.size();

        return keys.get(index);
    }

    private String maskKey(String key) {
        if (key == null || key.length() < 8) {
            return "****";
        }
        return key.substring(0, 4)
                + "..."
                + key.substring(key.length() - 4);
    }


    private List<Map<String, Object>> buildContents(
            String userMessage,
            List<ChatMessageDto> history) {

        List<Map<String, Object>> contents = new ArrayList<>();

        if (history != null) {
            history.stream()
                    .skip(Math.max(0, history.size() - 5))
                    .forEach(h -> {
                        contents.add(Map.of(
                                "role", "user",
                                "parts", List.of(
                                        Map.of("text", h.getMessage())
                                )
                        ));
                        contents.add(Map.of(
                                "role", "model",
                                "parts", List.of(
                                        Map.of("text", UtilFormat.extractOkContentForHistory(h.getResponse()))
                                )
                        ));
                    });
        }

        contents.add(Map.of(
                "role", "user",
                "parts", List.of(
                        Map.of("text", userMessage)
                )
        ));

        return contents;
    }

    private LoopOutcome runWithFallback(List<String> modelList, List<String> keyList,
                                        List<Map<String, Object>> initialContents, Long userId, String originalUserMessage) throws Exception {

        Exception lastException = null;

        for (String model : modelList) {
            int attempts = keyList.size();
            for (int i = 0; i < attempts; i++) {
                String apiKey = getNextApiKey(keyList);
                try {
                    log.info("Trying model={} key={}", model, maskKey(apiKey));
                    return continueLoop(model, apiKey, new ArrayList<>(initialContents), userId, 0, originalUserMessage);

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
        throw new GeminiUnavailableException("All Gemini models are unavailable", lastException);
    }

    private LoopOutcome continueLoop(String model, String apiKey, List<Map<String, Object>> contents,
                                     Long userId, int startRound, String originalUserMessage) throws Exception {

        for (int round = startRound; round < MAX_FUNCTION_CALL_ROUNDS; round++) {
            JsonNode root = callGeminiRaw(model, apiKey, contents, objectMapper);
            log.info("Root : \n {}",
                    objectMapper.writerWithDefaultPrettyPrinter()
                            .writeValueAsString(root)
            );

            JsonNode candidate = root.path("candidates").get(0);
            JsonNode parts = candidate.path("content").path("parts");

            JsonNode functionCallPart = null;

            for (JsonNode part : parts) {
                if (part.has("functionCall")) {
                    functionCallPart = part.path("functionCall");
                    break;
                }
            }

            if (functionCallPart == null) {
                StringBuilder text = new StringBuilder();
                for (JsonNode part : parts) {
                    if (part.has("text")) {
                        text.append(part.path("text").asText(""));
                    }
                }
                return LoopOutcome.finalText(text.toString());
            }

            String functionName = functionCallPart.path("name").asText();

            Map<String, Object> args = objectMapper.convertValue(functionCallPart.path("args"), Map.class);

            List<Map<String, Object>> contentsWithModelCall = new ArrayList<>(contents);

            Map<String, Object> modelContent =
                    objectMapper.convertValue(candidate.get("content"), Map.class);

            contentsWithModelCall.add(modelContent);

            // Tool bình thường -> thực thi ngay như cũ
            Object toolResult = executeTool(functionName, args, userId);

            contentsWithModelCall.add(functionResponseContent(functionName, toolResult));
            contents = contentsWithModelCall;
        }

        throw new GeminiUnavailableException(
                "Vượt quá số vòng gọi function cho phép (" + MAX_FUNCTION_CALL_ROUNDS + ")", null
        );
    }


    private JsonNode callGeminiRaw(
            String model,
            String apiKey,
            List<Map<String, Object>> contents,
            ObjectMapper mapper) throws Exception {

        RestTemplate restTemplate = new RestTemplate();

        Map<String, Object> body = new HashMap<>();

        // System instruction của Gemini
        body.put(
                "systemInstruction",
                Map.of(
                        "parts",
                        List.of(
                                Map.of("text", SYSTEM_TEMPLATE)
                        )
                )
        );

        // Conversation history + user message hiện tại
        body.put("contents", contents);

        // Function calling / tools
        body.put(
                "tools",
                List.of(toolDefinitions.buildToolsPayload())
        );

        // Log request
        String logJson = mapper
                .writerWithDefaultPrettyPrinter()
                .writeValueAsString(body);

        log.info("Gemini request:\n{}", logJson);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-goog-api-key", apiKey);

        HttpEntity<Map<String, Object>> request =
                new HttpEntity<>(body, headers);

        String url = baseUrl + "/" + model + ":generateContent";

        ResponseEntity<String> response =
                restTemplate.postForEntity(
                        url,
                        request,
                        String.class
                );

        return mapper.readTree(response.getBody());
    }

    private Object executeTool(String functionName, Map<String, Object> args, Long userId) {
        log.info("Executing tool={} args={} userId={}", functionName, args, userId);
        try {
            return switch (functionName) {

                case "searchJobs" -> chatToolService.searchJobs(
                        UtilFormat.asString(args.get("keyword")),
                        UtilFormat.asString(args.get("location")),
                        UtilFormat.asDouble(args.get("salaryMin")),
                        UtilFormat.asInteger(args.get("experienceYears")),
                        UtilFormat.asString(args.get("level")),
                        UtilFormat.asString(args.get("workMode")),
                        UtilFormat.asString(args.get("workingType")),
                        UtilFormat.asString(args.get("company")),
                        UtilFormat.asString(args.get("category"))

                );
                case "getJobDetail" -> chatToolService.getJobDetail(UtilFormat.asLong(args.get("jobId")));

                case "searchCompany" -> chatToolService.searchCompanies(
                        UtilFormat.asString(args.get("name")),
                        UtilFormat.asString(args.get("taxCode")),
                        UtilFormat.asString(args.get("email")),
                        UtilFormat.asString(args.get("phone")),
                        UtilFormat.asString(args.get("website")),
                        UtilFormat.asString(args.get("address")),
                        UtilFormat.asString(args.get("industry"))
                );
                case "getApplicationSummary" -> chatToolService.getApplicationSummary(userId);

                case "getAppliedJobs" -> chatToolService.getAppliedJobs(
                        userId,
                        UtilFormat.asString(args.get("status")),
                        UtilFormat.asString(args.get("keyword")),
                        UtilFormat.asInteger(args.get("limit"))
                );
                case "saveFavoriteJob" -> chatToolService.saveFavoriteJob(userId,
                        UtilFormat.asLong(args.get("jobId")));

                case "findMatchingJobs" -> chatToolService.geminiJobResponAI(userId);

                case "removeFavoriteJob" -> chatToolService.removeFavoriteJob(
                        userId
                        ,UtilFormat.asLong(args.get("jobId"))
                );
                case "getFavoriteJobs" -> chatToolService.getFavoriteJobs(
                        userId
                        ,UtilFormat.asInteger(args.get("limit"))
                );
                default -> Map.of("error", "Unknown tool: " + functionName);
            };
        } catch (Exception e) {
            log.error("Tool {} execution failed", functionName, e);
            return Map.of("error", "Lỗi khi thực thi thao tác: " + e.getMessage());
        }
    }

    private Map<String, Object> functionResponseContent(
            String functionName,
            Object toolResult
    ) {

        Map<String,Object> response = new HashMap<>();

        switch (functionName) {

            case "searchJobs":
                response.put("jobs", toolResult);
                break;

            case "getJobDetail":
                response.put("job", toolResult);
                break;

            case "searchCompany":
                response.put("company", toolResult);
                break;

            case "getApplicationSummary":
                response.put("summary", toolResult);
                break;

            case "getAppliedJobs":
                response.put("applies", toolResult);
                break;

            case "saveFavoriteJob":
                response.put("savejob", toolResult);
                break;

            case "findMatchingJobs":
                response.put("matchingJobs", toolResult);
                break;

            case "removeFavoriteJob":
                response.put("removeFavoriteJob", toolResult);
                break;

            case "getFavoriteJobs":
                response.put("getFavoriteJobs", toolResult);
                break;

            default:
                response.put("data", toolResult);
        }
        return Map.of(
                "role", "user",
                "parts", List.of(
                        Map.of(
                                "functionResponse",
                                Map.of(
                                        "name", functionName,
                                        "response", response
                                )
                        )
                )
        );
    }

    // Chuẩn hoá LoopOutcome -> ChatMessageDto trả về client (dùng chung cho cả 2 entry point)
    // ================================================================
    private ChatMessageDto finalizeOutcome(LoopOutcome outcome, User user, String originalUserMessage) {
        ChatMessageDto responseDto = new ChatMessageDto();
        responseDto.setMessage(originalUserMessage);
        responseDto.setTimestamp(LocalDateTime.now());


        // OutcomeType.FINAL_TEXT
        String aiResponse = outcome.getFinalText();

        if (isInvalidResponse(aiResponse)) {
            responseDto.setResponse("Xin lỗi, tôi chỉ hỗ trợ các câu hỏi liên quan đến tuyển dụng và tư vấn việc làm.");
            return responseDto;
        }
        String finalResponse = UtilFormat.extractOkContentForHistory(aiResponse);

        responseDto.setResponse(UtilFormat.extractOkContentForUser(finalResponse));
        saveMessage(user, originalUserMessage, "[OK]\n" + finalResponse);
        return responseDto;
    }





}
