package com.webjob.application.controller;

import com.webjob.application.annotation.RateLimit;
import com.webjob.application.dto.Request.Search.SubscriberFilterRequest;
import com.webjob.application.dto.Request.SubscriberSubscriptionRequest;
import com.webjob.application.dto.Response.ResponseDTO;
import com.webjob.application.dto.Response.SubscriberListResponse;
import com.webjob.application.dto.Response.SubscriberResponse;
import com.webjob.application.dto.Request.SubscriberRequest;
import com.webjob.application.dto.Response.ApiResponse;
import com.webjob.application.service.SubscriberService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/subscribers")
@RequiredArgsConstructor
public class SubscriberController {

    private final SubscriberService subscriberService;


    @RateLimit(maxRequests = 5, timeWindowSeconds = 60, keyType = "TOKEN")
    @PostMapping
    public ResponseEntity<ApiResponse<SubscriberResponse>> createSubscriber(@Valid @RequestBody SubscriberRequest request) {
        ApiResponse<SubscriberResponse> apiResponse = new ApiResponse<>(
                HttpStatus.CREATED.value(),
                null,
                "Tạo Subscriber thành công",
                subscriberService.createSubscriber(request)
        );
        return new ResponseEntity<>(apiResponse, HttpStatus.CREATED);
    }

    @RateLimit(maxRequests = 10, timeWindowSeconds = 60, keyType = "TOKEN")
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<SubscriberResponse>> updateSubscriber(@PathVariable Long id, @RequestBody SubscriberRequest request) {
        ApiResponse<SubscriberResponse> apiResponse = new ApiResponse<>(
                HttpStatus.OK.value(),
                null,
                "Update Subscriber thành công",
                subscriberService.updateSubscriber(id, request));
        return ResponseEntity.ok(apiResponse);

    }

    @RateLimit(maxRequests = 10, timeWindowSeconds = 60, keyType = "TOKEN")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Object>> deleteSubscriber(@PathVariable Long id) {
        subscriberService.deleteSubscriber(id);
        ApiResponse<Object> apiResponse = new ApiResponse<>(
                HttpStatus.OK.value(),
                null,
                "Xóa subscriber thành công",
                null
        );
        return ResponseEntity.ok(apiResponse);
    }

    @RateLimit(maxRequests = 10, timeWindowSeconds = 60, keyType = "TOKEN")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<SubscriberResponse>> detail(@PathVariable Long id) {

        ApiResponse<SubscriberResponse> apiResponse = new ApiResponse<>(
                HttpStatus.OK.value(),
                null,
                "Lấy Subscriber thành công",
                subscriberService.getDetail(id)
        );
        return ResponseEntity.ok(apiResponse);

    }


    @RateLimit(maxRequests = 20, timeWindowSeconds = 60, keyType = "TOKEN")
    @GetMapping()
    public ResponseEntity<ApiResponse<ResponseDTO<List<SubscriberListResponse>>>> getSubscriberSkills(
            @RequestParam(defaultValue = "0") int page
            , @RequestParam(defaultValue = "10") int size
            , @RequestParam(required = false) SubscriberFilterRequest request) {
        ApiResponse<ResponseDTO<List<SubscriberListResponse>>> apiResponse = new ApiResponse<>(
                HttpStatus.OK.value(),
                null,
                "Get All Skill Subscriber thành công",
                subscriberService.getAllSubscriber(page, size, request)
        );
        return ResponseEntity.ok(apiResponse);
    }

    @RateLimit(maxRequests = 20, timeWindowSeconds = 60, keyType = "TOKEN")
    @PatchMapping("/{id}/subscription")
    public ResponseEntity<ApiResponse<Object>> updateSubscription(
            @PathVariable Long id
            , @Valid @RequestBody SubscriberSubscriptionRequest request) {
        subscriberService.updateSubscription(id, request.getSubscribed());
        ApiResponse<Object> apiResponse = new ApiResponse<>(
                HttpStatus.OK.value(),
                null,
                "Cập nhật trạng thái nhận email thành công",
                null
        );
        return ResponseEntity.ok(apiResponse);
    }
}





