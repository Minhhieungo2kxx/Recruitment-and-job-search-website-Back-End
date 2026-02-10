package com.webjob.application.Controller;

import com.webjob.application.Annotation.RateLimit;
import com.webjob.application.Dto.Request.Chatbox.ChatMessageDto;
import com.webjob.application.Dto.Response.ApiResponse;
import com.webjob.application.Model.Entity.ChatMessage;
import com.webjob.application.Service.ChatBox.ChatService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/chat")
@Validated
public class ChatController {
    private static final Logger logger = LoggerFactory.getLogger(ChatController.class);
    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    @RateLimit(maxRequests = 20, timeWindowSeconds = 60, keyType = "TOKEN")
    @PostMapping("/send")
    public ResponseEntity<?> sendMessage(@Valid @RequestBody ChatMessageDto messageDto, Authentication authentication) {
        ChatMessageDto result = chatService.processMessage(messageDto, authentication);
        ApiResponse<?> apiResponse = ApiResponse.builder().
                statusCode(HttpStatus.OK.value()).
                message("Tin nhắn đã được xử lý thành công").
                error(null)
                .data(result)
                .build();
        return ResponseEntity.ok(apiResponse);
    }

    @RateLimit(maxRequests =40, timeWindowSeconds = 60, keyType = "TOKEN")
    @GetMapping("/history")
    public ResponseEntity<?> getChatHistory(Authentication authentication) {
        List<ChatMessageDto> history = chatService.getChatHistory(authentication);
        ApiResponse<?> apiResponse = ApiResponse.builder().
                statusCode(HttpStatus.OK.value()).
                message("Lay Lich su Tin nhắn đã được xử lý thành công").
                error(null)
                .data(history)
                .build();
        return ResponseEntity.ok(apiResponse);


    }

    @RateLimit(maxRequests = 3, timeWindowSeconds = 60, keyType = "TOKEN")
    @DeleteMapping("/history/clear")
    public ResponseEntity<?> clearChatHistory(Authentication authentication) {
        chatService.clearChatHistory(authentication);
        ApiResponse<?> apiResponse = ApiResponse.builder().
                statusCode(HttpStatus.OK.value()).
                message("Lịch sử chat đã được xóa thành công").
                error(null)
                .data(null)
                .build();
        return ResponseEntity.ok(apiResponse);


    }


}
