package com.webjob.application.controller;

import com.webjob.application.annotation.RateLimit;
import com.webjob.application.dto.Request.Websockets.ConversationResponseDTO;
import com.webjob.application.dto.Request.Websockets.MessageRequestDTO;
import com.webjob.application.dto.Request.Websockets.MessageUpdateDTO;
import com.webjob.application.dto.Request.Websockets.UserInfoDTO;
import com.webjob.application.dto.Response.Messensage.ApiResponseSocket;
import com.webjob.application.dto.Response.Messensage.MessageResponseDTO;
import com.webjob.application.service.Socket.MessageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/messages")
@Validated
public class MessageController {
    private final MessageService messageService;



    @RateLimit(maxRequests = 30, timeWindowSeconds = 60, keyType = "TOKEN")
    @PostMapping("/send")
    public ResponseEntity<?> sendMessage(
            @Valid @RequestBody MessageRequestDTO requestDTO,
            Authentication authentication) {
       return messageService.sendMessage(requestDTO,authentication);
    }

    @RateLimit(maxRequests = 10, timeWindowSeconds = 60, keyType = "TOKEN")
    @PutMapping("/update")
    public ResponseEntity<?> updateMessage(
            @Valid @RequestBody MessageUpdateDTO updateDTO,
            Authentication authentication) {

        return messageService.updateMessage(updateDTO,authentication);
    }

    @RateLimit(maxRequests = 10, timeWindowSeconds = 60, keyType = "TOKEN")
    @DeleteMapping("/{messageId}")
    public ResponseEntity<?> deleteMessage(@PathVariable Long messageId, Authentication authentication) {
        messageService.deleteMessage(authentication.getName(), messageId);
        return ResponseEntity.ok(ApiResponseSocket.success("Đã xóa tin nhắn thành công"));
    }

    @RateLimit(maxRequests = 20, timeWindowSeconds = 60, keyType = "TOKEN")
    @GetMapping("/conversation/{otherUserId}")
    public ResponseEntity<?> getConversation(
            @PathVariable Long otherUserId,
            Authentication authentication) {
        List<MessageResponseDTO> messages = messageService.getMessagesBetweenUsers(
                authentication.getName(), otherUserId);

        return ResponseEntity.ok(ApiResponseSocket.success(messages));
    }

    @RateLimit(maxRequests = 10, timeWindowSeconds = 60, keyType = "TOKEN")
    @GetMapping("/conversations")
    public ResponseEntity<?> getConversations(
            Authentication authentication) {

        List<ConversationResponseDTO> conversations = messageService.getUserConversations(authentication.getName());

        return ResponseEntity.ok(ApiResponseSocket.success(conversations));
    }

    @RateLimit(maxRequests = 25, timeWindowSeconds = 60, keyType = "IP")
    @GetMapping("/search-users")
    public ResponseEntity<?> searchUsers(
            @RequestParam String searchTerm,
            Authentication authentication) {

        List<UserInfoDTO> users = messageService.searchUsers(authentication.getName(), searchTerm);

        return ResponseEntity.ok(ApiResponseSocket.success(users));
    }

}
