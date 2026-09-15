package com.webjob.application.controller;

import com.webjob.application.annotation.RateLimit;
import com.webjob.application.dto.Response.ApiResponse;
import com.webjob.application.dto.record.JobRecommendationResponse;
import com.webjob.application.service.JobRecommendation.JobRecommendationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/recommendations")
@RequiredArgsConstructor
public class JobRecommendationController {
    private final JobRecommendationService recommendationService;

    @RateLimit(maxRequests = 30, timeWindowSeconds = 60, keyType = "TOKEN")
    @GetMapping("/jobs")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ApiResponse<JobRecommendationResponse>> getRecommendations() {
        ApiResponse<JobRecommendationResponse> apiResponse = new ApiResponse<>(
                HttpStatus.OK.value(),
                null,
                "Get job recommendations successfully",
                recommendationService.recommend()
        );
        return ResponseEntity.ok(apiResponse);
    }

    //     Bắt buộc tính lại (bỏ qua cache) — dùng khi user cập nhật CV mới
    @RateLimit(maxRequests = 4, timeWindowSeconds = 60, keyType = "TOKEN")
    @PostMapping("/jobs/refresh")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ApiResponse<JobRecommendationResponse>> refreshRecommendations() {
        JobRecommendationResponse data = recommendationService.forceRefresh();
        ApiResponse<JobRecommendationResponse> apiResponse = new ApiResponse<>(
                HttpStatus.OK.value(),
                null,
                "Refresh job recommendations successfully",
                data
        );

        return ResponseEntity.ok(apiResponse);
    }
}
