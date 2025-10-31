package com.webjob.application.Controller;

import com.webjob.application.Annotation.RateLimit;
import com.webjob.application.Config.Socket.MessageMapper;
import com.webjob.application.Model.Entity.Conversation;
import com.webjob.application.Dto.Request.Search.ConversationFilter;
import com.webjob.application.Dto.Response.ApiResponse;
import com.webjob.application.Dto.Response.ConversationDTO;
import com.webjob.application.Dto.Response.ResponseDTO;
import com.webjob.application.Service.ConversationService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/conversations")
public class ConversationController {
    private final ConversationService conversationService;
    private final MessageMapper messageMapper;

    public ConversationController(ConversationService conversationService, MessageMapper messageMapper) {
        this.conversationService = conversationService;
        this.messageMapper = messageMapper;
    }
    @RateLimit(maxRequests = 10, timeWindowSeconds = 60, keyType = "IP")
    @GetMapping
    public ResponseEntity<?> getAllConversations(ConversationFilter filter) {
        ResponseDTO<?> respond = conversationService.getPaginated(filter);
        ApiResponse<?> response = new ApiResponse<>(HttpStatus.OK.value(), null,
                "Get all Conversations Succesful",
                respond
        );
        return ResponseEntity.ok(response);
    }

    @RateLimit(maxRequests = 5, timeWindowSeconds = 60, keyType = "IP")
    @GetMapping("/{id}")
    public ResponseEntity<?> getConversationById(@PathVariable Long id) {
        Conversation conversation = conversationService.getbyID(id);
        ConversationDTO conversationDTO = messageMapper.toDTO(conversation);
        ApiResponse<?> response = new ApiResponse<>(HttpStatus.OK.value(), null,
                "Detail  Conversations Succesful with " + id,
                conversationDTO
        );
        return ResponseEntity.ok(response);


    }
    @RateLimit(maxRequests = 3, timeWindowSeconds = 60, keyType = "TOKEN")
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteConversation(@PathVariable Long id) {
        conversationService.deleteConversation(id);
        ApiResponse<?> response = new ApiResponse<>(HttpStatus.OK.value(), null,
                "Delete  Conversations Succesful with " + id,
                null
        );
        return ResponseEntity.ok(response);

    }
}
