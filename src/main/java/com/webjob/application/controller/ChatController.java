package com.webjob.application.controller;

import com.webjob.application.annotation.RateLimit;
import com.webjob.application.dto.Request.Chatbox.ChatMessageDto;
import com.webjob.application.dto.Response.ApiResponse;
import com.webjob.application.service.ChatBox.ChatService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/chat")
public class ChatController {
    private final ChatService chatService;


    @RateLimit(maxRequests = 20, timeWindowSeconds = 60, keyType = "TOKEN")
    @PostMapping("/send")
    public ResponseEntity<?> sendMessage(@Valid @RequestBody ChatMessageDto messageDto, Authentication authentication) {
        return chatService.sendMessage(messageDto, authentication);
    }

    @RateLimit(maxRequests = 40, timeWindowSeconds = 60, keyType = "TOKEN")
    @GetMapping("/history")
    public ResponseEntity<?> getChatHistory(Authentication authentication) {
        return chatService.ListChatHistory(authentication);
    }

    @RateLimit(maxRequests = 3, timeWindowSeconds = 60, keyType = "TOKEN")
    @DeleteMapping("/history/clear")
    public ResponseEntity<?> clearChatHistory(Authentication authentication) {
        return chatService.deleteChatHistory(authentication);
    }



}
