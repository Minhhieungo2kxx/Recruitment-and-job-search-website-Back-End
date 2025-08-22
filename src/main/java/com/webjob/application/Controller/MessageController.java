package com.webjob.application.Controller;

import com.webjob.application.Models.Request.Websockets.ConversationResponseDTO;
import com.webjob.application.Models.Request.Websockets.MessageRequestDTO;
import com.webjob.application.Models.Request.Websockets.MessageUpdateDTO;
import com.webjob.application.Models.Request.Websockets.UserInfoDTO;
import com.webjob.application.Models.Response.Messensage.ApiResponseSocket;
import com.webjob.application.Models.Response.Messensage.MessageResponseDTO;
import com.webjob.application.Services.Socket.MessageService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/messages")
@Validated
public class MessageController {
    private final MessageService messageService;
    private final SimpMessagingTemplate messagingTemplate;

    public MessageController(MessageService messageService,
                             SimpMessagingTemplate messagingTemplate) {
        this.messageService = messageService;
        this.messagingTemplate = messagingTemplate;
    }

    @PostMapping("/send")
    public ResponseEntity<?> sendMessage(
            @Valid @RequestBody MessageRequestDTO requestDTO,
            Authentication authentication) {

        MessageResponseDTO message = messageService.sendMessage(authentication.getName(), requestDTO);

        // Gửi tin nhắn qua WebSocket đến người nhận
        messagingTemplate.convertAndSendToUser(
                message.getReceiver().getEmail(),
                "/queue/messages",
                message
        );
        // Gửi lại tin nhắn cho chính người gửi
        messagingTemplate.convertAndSendToUser(
                message.getSender().getEmail(),  // <-- dòng này là bổ sung
                "/queue/messages",
                message
        );

        return ResponseEntity.ok(ApiResponseSocket.success(message));
    }

    @PutMapping("/update")
    public ResponseEntity<?> updateMessage(
            @Valid @RequestBody MessageUpdateDTO updateDTO,
            Authentication authentication) {

        MessageResponseDTO updatedMessage = messageService.updateMessage(authentication.getName(), updateDTO);

        // Thông báo cập nhật qua WebSocket
        messagingTemplate.convertAndSendToUser(
                updatedMessage.getReceiver().getEmail(),
                "/queue/message-updates",
                updatedMessage
        );
        messagingTemplate.convertAndSendToUser(
                updatedMessage.getSender().getEmail(),
                "/queue/message-updates",
                updatedMessage
        );

        return ResponseEntity.ok(ApiResponseSocket.success(updatedMessage));
    }

    @DeleteMapping("/{messageId}")
    public ResponseEntity<?> deleteMessage(
            @PathVariable Long messageId,
            Authentication authentication) {

        messageService.deleteMessage(authentication.getName(), messageId);

        return ResponseEntity.ok(ApiResponseSocket.success("Đã xóa tin nhắn thành công"));
    }

    @GetMapping("/conversation/{otherUserId}")
    public ResponseEntity<?> getConversation(
            @PathVariable Long otherUserId,
            Authentication authentication) {

        List<MessageResponseDTO> messages = messageService.getMessagesBetweenUsers(
                authentication.getName(), otherUserId);

        return ResponseEntity.ok(ApiResponseSocket.success(messages));
    }

    @GetMapping("/conversations")
    public ResponseEntity<?> getConversations(
            Authentication authentication) {

        List<ConversationResponseDTO> conversations = messageService.getUserConversations(authentication.getName());

        return ResponseEntity.ok(ApiResponseSocket.success(conversations));
    }

    @GetMapping("/search-users")
    public ResponseEntity<?> searchUsers(
            @RequestParam String searchTerm,
            Authentication authentication) {

        List<UserInfoDTO> users = messageService.searchUsers(authentication.getName(), searchTerm);

        return ResponseEntity.ok(ApiResponseSocket.success(users));
    }

}
