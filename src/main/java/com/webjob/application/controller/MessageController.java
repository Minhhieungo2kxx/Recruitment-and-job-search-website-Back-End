package com.webjob.application.controller;

import com.webjob.application.annotation.RateLimit;
import com.webjob.application.dto.Request.Search.MessageFilterRequest;
import com.webjob.application.dto.Request.Websockets.ConversationResponseDTO;
import com.webjob.application.dto.Request.Websockets.MessageRequestDTO;
import com.webjob.application.dto.Request.Websockets.MessageUpdateDTO;
import com.webjob.application.dto.Request.Websockets.UserInfoDTO;
import com.webjob.application.dto.Response.ApiResponse;
import com.webjob.application.dto.Response.MessagesDTO;
import com.webjob.application.dto.Response.Messensage.ApiResponseSocket;
import com.webjob.application.dto.Response.Messensage.MessageResponseDTO;
import com.webjob.application.dto.Response.ResponseDTO;
import com.webjob.application.service.Socket.MessageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
    public ResponseEntity<ApiResponseSocket<MessageResponseDTO>> sendMessage(
            @Valid @RequestBody MessageRequestDTO requestDTO, Authentication authentication) {
        return ResponseEntity.ok(ApiResponseSocket.success(messageService.sendMessage(requestDTO, authentication)));

    }

    @RateLimit(maxRequests = 10, timeWindowSeconds = 60, keyType = "TOKEN")
    @PutMapping("/update")
    public ResponseEntity<ApiResponseSocket<MessageResponseDTO>> updateMessage(
            @Valid @RequestBody MessageUpdateDTO updateDTO,
            Authentication authentication) {
        return ResponseEntity.ok(ApiResponseSocket.success(messageService.updateMessage(updateDTO, authentication)));

    }

    @RateLimit(maxRequests = 10, timeWindowSeconds = 60, keyType = "TOKEN")
    @DeleteMapping("/{messageId}")
    public ResponseEntity<Object> deleteMessage(@PathVariable Long messageId, Authentication authentication) {
        messageService.deleteMessage(authentication.getName(), messageId);
        return ResponseEntity.ok(ApiResponseSocket.success("Đã xóa tin nhắn thành công"));
    }

    @RateLimit(maxRequests = 20, timeWindowSeconds = 60, keyType = "TOKEN")
    @GetMapping("/conversation/{otherUserId}")
    public ResponseEntity<ApiResponseSocket<List<MessageResponseDTO>>> getConversation(
            @PathVariable Long otherUserId,
            Authentication authentication) {
        List<MessageResponseDTO> messages = messageService.getMessagesBetweenUsers(
                authentication.getName(), otherUserId);

        return ResponseEntity.ok(ApiResponseSocket.success(messages));
    }

    @RateLimit(maxRequests = 10, timeWindowSeconds = 60, keyType = "TOKEN")
    @GetMapping("/conversations")
    public ResponseEntity<ApiResponseSocket<List<ConversationResponseDTO>>> getConversations(
            Authentication authentication) {

        List<ConversationResponseDTO> conversations = messageService.getUserConversations(authentication.getName());

        return ResponseEntity.ok(ApiResponseSocket.success(conversations));
    }

    @RateLimit(maxRequests = 25, timeWindowSeconds = 60, keyType = "IP")
    @GetMapping("/search-users")
    public ResponseEntity<ApiResponseSocket<List<UserInfoDTO>>> searchUsers(
            @RequestParam String searchTerm,
            Authentication authentication) {

        List<UserInfoDTO> users = messageService.searchUsers(authentication.getName(), searchTerm);

        return ResponseEntity.ok(ApiResponseSocket.success(users));
    }

    @RateLimit(maxRequests = 20, timeWindowSeconds = 60, keyType = "TOKEN")
    @GetMapping("/admin")
    public ResponseEntity<ApiResponse<ResponseDTO<List<MessagesDTO>>>> getAllMessages(
            @RequestParam(defaultValue = "0") int page
            , @RequestParam(defaultValue = "10") int size
            , @RequestBody(required = false) @Valid MessageFilterRequest filterRequest) {
        ApiResponse<ResponseDTO<List<MessagesDTO>>> response = new ApiResponse<>(
                HttpStatus.OK.value(),
                null,
                "Get all Messages Succesful",
                messageService.getAllMessages(page, size, filterRequest)
        );
        return ResponseEntity.ok(response);

    }

    @RateLimit(maxRequests = 5, timeWindowSeconds = 60, keyType = "TOKEN")
    @DeleteMapping("/{id}/admin")
    public ResponseEntity<Void> deleteMessage(@PathVariable Long id) {

        return messageService.deleteMessage(id);
    }

}
