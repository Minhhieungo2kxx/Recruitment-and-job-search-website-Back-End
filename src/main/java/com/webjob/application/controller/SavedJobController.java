package com.webjob.application.controller;

import com.webjob.application.annotation.RateLimit;
import com.webjob.application.dto.Response.ApiResponse;
import com.webjob.application.dto.Response.ResponseDTO;
import com.webjob.application.dto.Response.SavedJobResponse;
import com.webjob.application.service.SavedJobService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/saved-jobs")
@RequiredArgsConstructor
public class SavedJobController {
    private final SavedJobService savedJobService;

    @RateLimit(maxRequests = 8, timeWindowSeconds = 60, keyType = "TOKEN")
    @PreAuthorize("hasAnyRole('USER_BASIC','USER')")
//    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/{jobId}")
    public ResponseEntity<ApiResponse<Object>> saveJob(@PathVariable Long jobId) {
        savedJobService.saveJob(jobId);
        ApiResponse<Object> apiResponse = new ApiResponse<>(
                HttpStatus.CREATED.value(),
                null,
                "Saved job thành công",
                null
        );
        return new ResponseEntity<>(apiResponse, HttpStatus.CREATED);
    }

    @RateLimit(maxRequests = 5, timeWindowSeconds = 60, keyType = "TOKEN")
    @PreAuthorize("hasAnyRole('USER_BASIC','USER')")
    @DeleteMapping("/{jobId}")
    public ResponseEntity<ApiResponse<Object>> unsaveJob(@PathVariable Long jobId) {
        savedJobService.unsaveJob(jobId);

        ApiResponse<Object> apiResponse = new ApiResponse<>(
                HttpStatus.OK.value(),
                null,
                "Delete SavedJob thành công",
                null
        );
        return new ResponseEntity<>(apiResponse, HttpStatus.OK);
    }

    @RateLimit(maxRequests = 20, timeWindowSeconds = 60, keyType = "TOKEN")
    @PreAuthorize("hasAnyRole('USER_BASIC','USER')")
    @GetMapping
    public ResponseEntity<ApiResponse<ResponseDTO<List<SavedJobResponse>>>> getSavedJobs(
            @RequestParam(defaultValue = "1") int page
            , @RequestParam(defaultValue = "10") int size) {

        ApiResponse<ResponseDTO<List<SavedJobResponse>>> response = new ApiResponse<>(
                HttpStatus.OK.value(),
                null
                , "fetch all SaveJobs Successful"
                , savedJobService.getSavedJobs(page, size)
        );
        return ResponseEntity.ok(response);

    }


}

//feat(saved-job): implement saved job management APIs
