package com.webjob.application.controller;

import com.webjob.application.annotation.RateLimit;
import com.webjob.application.dto.Request.Search.MessageFilterRequest;
import com.webjob.application.dto.Response.ApiResponse;
import com.webjob.application.dto.Response.ResponseDTO;
import com.webjob.application.service.Socket.MessageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/ap1/v1/admin/messages")
@RequiredArgsConstructor
public class AdminMessageController {
    private final MessageService messageService;


    @RateLimit(maxRequests = 20, timeWindowSeconds = 60, keyType = "TOKEN")
    @GetMapping
    public ResponseEntity<?> getAllMessages(@ModelAttribute MessageFilterRequest filterRequest) {
       return messageService.getAllMessages(filterRequest);
    }
    @RateLimit(maxRequests = 5, timeWindowSeconds = 60, keyType = "TOKEN")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteMessage(@PathVariable Long id) {
        return messageService.deleteMessage(id);
    }
}
