package com.webjob.application.controller;

import com.webjob.application.annotation.RateLimit;

import com.webjob.application.dto.Request.Search.ConversationFilter;
import com.webjob.application.dto.Response.ApiResponse;

import com.webjob.application.service.ConversationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/admin/conversations")
public class ConversationController {
    private final ConversationService conversationService;


    @RateLimit(maxRequests = 10, timeWindowSeconds = 60, keyType = "IP")
    @GetMapping
    public ResponseEntity<?> getAllConversations(ConversationFilter filter) {
        return conversationService.getAllConversations(filter);
    }

    @RateLimit(maxRequests = 5, timeWindowSeconds = 60, keyType = "IP")
    @GetMapping("/{id}")
    public ResponseEntity<?> getConversationById(@PathVariable Long id) {
        return conversationService.getConversationById(id);

    }
    @RateLimit(maxRequests = 3, timeWindowSeconds = 60, keyType = "TOKEN")
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteConversation(@PathVariable Long id) {
        return conversationService.deleteConversationById(id);

    }
}
