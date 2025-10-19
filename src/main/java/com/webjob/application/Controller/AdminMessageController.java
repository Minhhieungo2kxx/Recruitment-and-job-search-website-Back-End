package com.webjob.application.Controller;

import com.webjob.application.Annotation.RateLimit;
import com.webjob.application.Configs.Socket.MessageMapper;
import com.webjob.application.Models.Entity.Job;
import com.webjob.application.Models.Request.Search.MessageFilterRequest;
import com.webjob.application.Models.Response.ApiResponse;
import com.webjob.application.Models.Response.ResponseDTO;
import com.webjob.application.Repository.MessageRepository;
import com.webjob.application.Services.Socket.MessageService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/ap1/v1/admin/messages")

public class AdminMessageController {
    private final MessageService messageService;


    public AdminMessageController(MessageService messageService) {
        this.messageService = messageService;
    }

    @RateLimit(maxRequests = 20, timeWindowSeconds = 60, keyType = "TOKEN")
    @GetMapping
    public ResponseEntity<?> getAllMessages(@ModelAttribute MessageFilterRequest filterRequest) {
        ResponseDTO<?> respond=messageService.getPaginated(filterRequest);
        ApiResponse<?> response=new ApiResponse<>(HttpStatus.OK.value(), null,
                "Get all Messages Succesful",
                respond
        );
        return ResponseEntity.ok(response);
    }
    @RateLimit(maxRequests = 5, timeWindowSeconds = 60, keyType = "TOKEN")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteMessage(@PathVariable Long id) {
        boolean deleted = messageService.softDeleteMessage(id);
        return deleted ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build();
    }
}
