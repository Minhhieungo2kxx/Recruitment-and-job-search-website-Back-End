package com.webjob.application.controller;

import com.webjob.application.annotation.RateLimit;
import com.webjob.application.dto.Response.ApiResponse;

import com.webjob.application.dto.Response.JobApplicantInfoResponse;
import com.webjob.application.dto.Response.JobResponse;

import com.webjob.application.dto.Request.JobRequest;


import com.webjob.application.dto.Response.ResponseDTO;
import com.webjob.application.dto.Request.JobFilterAdminRequest;
import com.webjob.application.dto.Request.JobFilterClient;
import com.webjob.application.dto.Request.JobFilterHrRequest;
import com.webjob.application.service.JobService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/jobs") // Base URL  RESTful
@Slf4j
public class JobController {
    private final JobService jobService;


    @RateLimit(maxRequests = 5, timeWindowSeconds = 60, keyType = "TOKEN")
    @PostMapping
    public ResponseEntity<ApiResponse<JobResponse>> createJob(@Valid @RequestBody JobRequest request) {
        ApiResponse<JobResponse> apiResponse = new ApiResponse<>(
                HttpStatus.CREATED.value(), null,
                "Tạo job thành công",
                jobService.createJob(request));
        return new ResponseEntity<>(apiResponse, HttpStatus.CREATED);
    }

    @RateLimit(maxRequests = 5, timeWindowSeconds = 60, keyType = "TOKEN")
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<JobResponse>> editJob(@PathVariable Long id, @Valid @RequestBody JobRequest request) {
        ApiResponse<JobResponse> apiResponse = new ApiResponse<>(
                HttpStatus.OK.value(),
                null,
                "Update job thành công",
                jobService.updateJob(id, request));
        return new ResponseEntity<>(apiResponse, HttpStatus.OK);

    }

    @RateLimit(maxRequests = 10, timeWindowSeconds = 60, keyType = "IP")
    @GetMapping("/admin")
    public ResponseEntity<ApiResponse<ResponseDTO<List<JobResponse>>>> getAllJobsForAdmin(
            @RequestParam(defaultValue = "1") int page
            , @RequestParam(defaultValue = "10") int size
            , @RequestBody(required = false) JobFilterAdminRequest request) {
        ApiResponse<ResponseDTO<List<JobResponse>>> response = new ApiResponse<>(
                HttpStatus.OK.value(),
                null
                , "fetch all Jobs"
                , jobService.getAllAdmin(page, size, request)
        );
        return ResponseEntity.ok(response);

    }

    @RateLimit(maxRequests = 15, timeWindowSeconds = 60, keyType = "IP")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<JobResponse>> detailJob(@PathVariable Long id) {
        ApiResponse<JobResponse> apiResponse = new ApiResponse<>(
                HttpStatus.OK.value(),
                null,
                "Detail job thành công with " + id,
                jobService.detailJobId(id));
        return new ResponseEntity<>(apiResponse, HttpStatus.OK);

    }

    @RateLimit(maxRequests = 5, timeWindowSeconds = 60, keyType = "TOKEN")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Object>> deleteJobbyId(@PathVariable Long id) {
        jobService.deleteJob(id);
        ApiResponse<Object> response = new ApiResponse<>(
                HttpStatus.OK.value(),
                null,
                "Delete Job successful with " + id,
                null
        );
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @PutMapping("/{id}/restore")
    public ResponseEntity<ApiResponse<Object>> restoreJob(@PathVariable Long id) {
        jobService.restoreJob(id);
        ApiResponse<Object> response = new ApiResponse<>(
                HttpStatus.OK.value(),
                null,
                "Khôi phục công việc thành công. ",
                null
        );
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }


    @RateLimit(maxRequests = 10, timeWindowSeconds = 60, keyType = "TOKEN")
    @GetMapping("/{jobId}/applicant-info")
    public ResponseEntity<ApiResponse<JobApplicantInfoResponse>> getJobApplicantInfo(@PathVariable Long jobId) {
        ApiResponse<JobApplicantInfoResponse> apiResponse = new ApiResponse<>(
                HttpStatus.OK.value(),
                null,
                "Lấy thông tin ứng viên thành công",
                jobService.getJobApplicantInfo(jobId)
        );
        return ResponseEntity.ok(apiResponse);
    }

    @RateLimit(maxRequests = 10, timeWindowSeconds = 60, keyType = "TOKEN")
    @GetMapping("/my-company")
    public ResponseEntity<ApiResponse<ResponseDTO<List<JobResponse>>>> getJobsForCompany(
            @RequestParam(defaultValue = "0") int page
            , @RequestParam(defaultValue = "10") int size
            , @RequestBody (required = false) JobFilterHrRequest request) {
        ApiResponse<ResponseDTO<List<JobResponse>>> apiResponse = new ApiResponse<>(
                HttpStatus.OK.value(),
                null,
                "Lấy thông tin Job for Company thành công",
                jobService.getMyCompanyJobs(page, size, request)
        );
        return ResponseEntity.ok(apiResponse);

    }


    @RateLimit(maxRequests = 20, timeWindowSeconds = 60, keyType = "IP")
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<ResponseDTO<List<JobResponse>>>> getPublicJobs(
            @RequestParam(defaultValue = "1") int page
            , @RequestParam(defaultValue = "10") int size
            , @RequestBody(required = false) JobFilterClient request) {

        ApiResponse<ResponseDTO<List<JobResponse>>> apiResponse = new ApiResponse<>(
                HttpStatus.OK.value(),
                null,
                "Lấy thông tin Job Filter thành công",
                jobService.searchJob(page, size, request)
        );
        return ResponseEntity.ok(apiResponse);


    }


}
