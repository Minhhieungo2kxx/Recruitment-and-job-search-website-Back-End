package com.webjob.application.controller;

import com.webjob.application.annotation.RateLimit;
import com.webjob.application.dto.Response.ApiResponse;
import com.webjob.application.dto.Response.JobApplicantInfoResponse;

import com.webjob.application.dto.Response.ResponseDTO;

import com.webjob.application.models.Entity.Job;
import com.webjob.application.models.Entity.User;
import com.webjob.application.dto.Request.JobRequest;
import com.webjob.application.dto.Request.Search.JobFiltersearch;


import com.webjob.application.service.JobService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/jobs") // Base URL chuẩn RESTful
@Slf4j
public class JobController {
    private final JobService jobService;



    @RateLimit(maxRequests = 5, timeWindowSeconds = 60, keyType = "TOKEN")
    @PostMapping
    public ResponseEntity<?> createJob(@Valid @RequestBody JobRequest request) {
        return jobService.create_newJob(request);
    }
    @RateLimit(maxRequests = 5, timeWindowSeconds = 60, keyType = "TOKEN")
    @PutMapping("/{id}")
    public ResponseEntity<?> editJob(@PathVariable Long id, @Valid @RequestBody JobRequest request) {
        return jobService.edit_Job(id,request);

    }
    @RateLimit(maxRequests = 10, timeWindowSeconds = 60, keyType = "IP")
    @GetMapping
    public ResponseEntity<?> GetallPageList(@ModelAttribute JobFiltersearch jobFiltersearch){
        return jobService.GetallPageList(jobFiltersearch);

    }

    @RateLimit(maxRequests = 15, timeWindowSeconds = 60, keyType = "IP")
    @GetMapping("/{id}")
    public ResponseEntity<?> detailJob(@PathVariable Long id) {
        return jobService.detailJob_Id(id);
    }

    @RateLimit(maxRequests = 5, timeWindowSeconds = 60, keyType = "TOKEN")
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteJobbyId(@PathVariable Long id) {
           return jobService.deleteJob_byId(id);
    }

    @RateLimit(maxRequests = 10, timeWindowSeconds = 60, keyType = "IP")
    @GetMapping("/search")
    public ResponseEntity<?> GetallSearch(@ModelAttribute JobFiltersearch jobFiltersearch){
       return jobService.GetallSearch_Job(jobFiltersearch);

    }

    @RateLimit(maxRequests = 10, timeWindowSeconds = 60, keyType = "TOKEN")
    @GetMapping("/{jobId}/applicant-info")
    public ResponseEntity<?> getJobApplicantInfo(@PathVariable Long jobId) {
            return jobService.getJob_ApplicantInfo(jobId);

    }


}
