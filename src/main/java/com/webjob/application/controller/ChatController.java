package com.webjob.application.controller;

import com.webjob.application.annotation.RateLimit;
import com.webjob.application.dto.Request.Chatbox.ChatMessageDto;
import com.webjob.application.dto.Response.ApiResponse;
import com.webjob.application.service.ChatBox.ChatService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/chat")
public class ChatController {
    private final ChatService chatService;


    @RateLimit(maxRequests = 20, timeWindowSeconds = 60, keyType = "TOKEN")
    @PostMapping("/send")
    public ResponseEntity<ApiResponse<Object>> sendMessage(@Valid @RequestBody ChatMessageDto messageDto, Authentication authentication) {
        ApiResponse<Object> apiResponse = ApiResponse.builder().
                statusCode(HttpStatus.OK.value()).
                message("Tin nhắn đã được xử lý thành công").
                error(null)
                .data(chatService.sendMessage(messageDto, authentication))
                .build();
        return ResponseEntity.ok(apiResponse);
    }

    @RateLimit(maxRequests = 40, timeWindowSeconds = 60, keyType = "TOKEN")
    @GetMapping("/history")
    public ResponseEntity<ApiResponse<Object>> getChatHistory(Authentication authentication) {
        ApiResponse<Object> apiResponse = ApiResponse.builder().
                statusCode(HttpStatus.OK.value()).
                message("Lay Lich su Tin nhắn đã được xử lý thành công").
                error(null)
                .data(chatService.ListChatHistory(authentication))
                .build();
        return ResponseEntity.ok(apiResponse);
    }

    @RateLimit(maxRequests = 3, timeWindowSeconds = 60, keyType = "TOKEN")
    @DeleteMapping("/history/clear")
    public ResponseEntity<ApiResponse<Object>> clearChatHistory(Authentication authentication) {
        chatService.deleteChatHistory(authentication);
        ApiResponse<Object> apiResponse = ApiResponse.builder().
                statusCode(HttpStatus.OK.value()).
                message("Lịch sử chat đã được xóa thành công").
                error(null)
                .data(null)
                .build();
        return ResponseEntity.ok(apiResponse);

    }



}
