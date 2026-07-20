package com.webjob.application.controller;

import com.webjob.application.annotation.RateLimit;

import com.webjob.application.dto.Request.Search.ConversationFilter;
import com.webjob.application.dto.Response.ApiResponse;

import com.webjob.application.dto.Response.ConversationDTO;
import com.webjob.application.dto.Response.ResponseDTO;
import com.webjob.application.service.ConversationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/conversations")
public class ConversationController {
    private final ConversationService conversationService;


    @RateLimit(maxRequests = 10, timeWindowSeconds = 60, keyType = "TOKEN")
    @GetMapping("/admin")
    public ResponseEntity<ApiResponse<ResponseDTO<List<ConversationDTO>>>> getAllConversations(
            @RequestParam(defaultValue = "1") int page
            , @RequestParam(defaultValue = "10") int size
            , @RequestParam(required = false) ConversationFilter filter) {
        ApiResponse<ResponseDTO<List<ConversationDTO>>> response = new ApiResponse<>(
                HttpStatus.OK.value(),
                null,
                "Get all Conversations Succesful",
                conversationService.getAllConversations(page, size, filter)
        );
        return ResponseEntity.ok(response);
    }

    @RateLimit(maxRequests = 5, timeWindowSeconds = 60, keyType = "TOKEN")
    @GetMapping("/{id}/admin")
    public ResponseEntity<ApiResponse<ConversationDTO>> getConversationById(
            @PathVariable Long id) {
        ApiResponse<ConversationDTO> response = new ApiResponse<>(
                HttpStatus.OK.value(),
                null,
                "Detail  Conversations Succesful with " + id,
                conversationService.getConversationById(id)
        );
        return ResponseEntity.ok(response);

    }

    @RateLimit(maxRequests = 3, timeWindowSeconds = 60, keyType = "TOKEN")
    @DeleteMapping("/{id}/admin")
    public ResponseEntity<ApiResponse<Object>> deleteConversation(@PathVariable Long id) {
        conversationService.deleteConversationById(id);
        ApiResponse<Object> response = new ApiResponse<>(
                HttpStatus.OK.value(),
                null,
                "Delete  Conversations Succesful with " + id,
                null
        );
        return ResponseEntity.ok(response);

    }
}
