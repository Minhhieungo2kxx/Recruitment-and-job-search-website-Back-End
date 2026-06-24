package com.webjob.application.controller;

import com.webjob.application.annotation.RateLimit;
import com.webjob.application.models.Entity.Subscriber;
import com.webjob.application.dto.Request.SubscriberRequest;
import com.webjob.application.dto.Response.ApiResponse;
import com.webjob.application.service.SubscriberService;
import com.webjob.application.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/subscribers")
@RequiredArgsConstructor
public class SubscriberController {

    private final SubscriberService subscriberService;



    @RateLimit(maxRequests = 5, timeWindowSeconds = 60, keyType = "IP")
    @PostMapping
    public ResponseEntity<?> createSubcriber(@Valid @RequestBody Subscriber subscriber) {
        Subscriber save=subscriberService.createSubciber(subscriber);
        ApiResponse<?> apiResponse=new ApiResponse<>(HttpStatus.CREATED.value(), null,
                "Tạo Subscriber thành công",
                save);
        return new ResponseEntity<>(apiResponse,HttpStatus.CREATED);

    }

    @RateLimit(maxRequests = 10, timeWindowSeconds = 60, keyType = "TOKEN")
    @PutMapping("/{id}")
    public ResponseEntity<?> editSubcriber(@PathVariable Long id, @RequestBody SubscriberRequest request) {
        Subscriber edit=subscriberService.updateSubciber(request);
        ApiResponse<?> apiResponse=new ApiResponse<>(HttpStatus.CREATED.value(), null,
                "Update Subscriber thành công",
                edit);
        return ResponseEntity.ok(apiResponse);

    }


    @RateLimit(maxRequests = 30, timeWindowSeconds = 60, keyType = "IP")
    @GetMapping("/skills")
    public ResponseEntity<?> GetSkillSubcriber(Authentication authentication) {
        Subscriber subscriber=subscriberService.getbySkillSub(authentication);
        ApiResponse<?> apiResponse=new ApiResponse<>(HttpStatus.OK.value(), null,
                "Get All Skill Subscriber thành công",
                subscriber);
        return ResponseEntity.ok(apiResponse);
    }


    @RateLimit(maxRequests = 3, timeWindowSeconds = 300, keyType = "TOKEN")
    @GetMapping("/send-mails")
    public ResponseEntity<?> SendEmail() {
            subscriberService.sendSubscribersEmailJobs();
            return ResponseEntity.ok(new ApiResponse<>(HttpStatus.OK.value(), null,
                    "Send Email Subscriber with Test Successful", null));
    }








}
