package com.webjob.application.controller;

import com.webjob.application.annotation.RateLimit;
import com.webjob.application.dto.Request.JobAlertRequest;
import com.webjob.application.dto.Response.ApiResponse;
import com.webjob.application.dto.Response.JobAlertResponse;
import com.webjob.application.dto.Response.ResponseDTO;
import com.webjob.application.service.JobAlertService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/job-alerts")
@RequiredArgsConstructor
@Tag(name = "Job Alert")
public class JobAlertController {
    private final JobAlertService jobAlertService;
    @RateLimit(maxRequests = 5, timeWindowSeconds = 60, keyType = "TOKEN")
    @PostMapping
    public ResponseEntity<ApiResponse<JobAlertResponse>> create(@Valid @RequestBody JobAlertRequest request) {
        ApiResponse<JobAlertResponse> apiResponse = new ApiResponse<>(
                HttpStatus.CREATED.value(),
                null,
                "Tạo Job Alert thành công",
                jobAlertService.create(request)
        );
        return new ResponseEntity<>(apiResponse, HttpStatus.CREATED);
    }

    @RateLimit(maxRequests = 30, timeWindowSeconds = 60, keyType = "TOKEN")
    @GetMapping
    public ResponseEntity<ApiResponse<ResponseDTO<List<JobAlertResponse>>>> getMyAlerts(
            @RequestParam(defaultValue = "0") int page
            ,@RequestParam(defaultValue = "10") int size
    ){
        ApiResponse<ResponseDTO<List<JobAlertResponse>>> apiResponse = new ApiResponse<>(
                HttpStatus.OK.value(),
                null,
                "Get  Job Alert By Id thành công",
                jobAlertService.getMyAlerts(page,size)
        );
        return ResponseEntity.ok(apiResponse);

    }

    @RateLimit(maxRequests = 30, timeWindowSeconds = 60, keyType = "TOKEN")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<JobAlertResponse>> getById(@PathVariable Long id) {
        ApiResponse<JobAlertResponse> apiResponse = new ApiResponse<>(
                HttpStatus.OK.value(),
                null,
                "Get  Job Alert By Id thành công",
                jobAlertService.getById(id)
        );
        return ResponseEntity.ok(apiResponse);
    }

    @RateLimit(maxRequests = 10, timeWindowSeconds = 60, keyType = "TOKEN")
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<JobAlertResponse>> update(@PathVariable Long id
            ,@Valid @RequestBody JobAlertRequest request) {
        ApiResponse<JobAlertResponse> apiResponse = new ApiResponse<>(
                HttpStatus.OK.value(),
                null,
                "Update Job Alert thành công",
                jobAlertService.update(id,request)
        );
        return ResponseEntity.ok(apiResponse);
    }

    @RateLimit(maxRequests = 10, timeWindowSeconds = 60, keyType = "TOKEN")
    @PatchMapping("/{id}/enable")
    public ResponseEntity<ApiResponse<Object>> enable(@PathVariable Long id) {

        jobAlertService.enable(id);
        ApiResponse<Object> apiResponse = new ApiResponse<>(
                HttpStatus.OK.value(),
                null,
                "Enable Active Job Alert thành công",
               null
        );

        return ResponseEntity.ok(apiResponse);
    }

    @RateLimit(maxRequests = 10, timeWindowSeconds = 60, keyType = "TOKEN")
    @PatchMapping("/{id}/disable")
    public ResponseEntity<ApiResponse<Object>> disable(@PathVariable Long id) {

        jobAlertService.disable( id);
        ApiResponse<Object> apiResponse = new ApiResponse<>(
                HttpStatus.OK.value(),
                null,
                "Disable Job Alert thành công",
                null
        );

        return ResponseEntity.ok(apiResponse);

    }

    @RateLimit(maxRequests = 5, timeWindowSeconds = 60, keyType = "TOKEN")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Object>> delete(@PathVariable Long id) {
        jobAlertService.delete(id);
        ApiResponse<Object> apiResponse = new ApiResponse<>(
                HttpStatus.OK.value(),
                null,
                "Deleted Job Alert thành công",
                null
        );
        return ResponseEntity.ok(apiResponse);
    }


}
