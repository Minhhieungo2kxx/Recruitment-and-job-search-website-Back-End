package com.webjob.application.controller;

import com.webjob.application.annotation.RateLimit;
import com.webjob.application.dto.Request.ResumeRequest;
import com.webjob.application.dto.Request.UpdateResumeUser;
import com.webjob.application.dto.Response.ApiResponse;
import com.webjob.application.dto.Response.ResponseDTO;
import com.webjob.application.dto.Response.ResumeResponse;
import com.webjob.application.enums.ResumeStatus;
import com.webjob.application.dto.Request.UpdateResumeHR;

import com.webjob.application.models.Entity.Resume;
import com.webjob.application.service.ResumService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

@RestController
@RequestMapping("/api/v1/resumes")
@RequiredArgsConstructor
public class ResumController {
    private final ResumService resumService;


    @RateLimit(maxRequests = 5, timeWindowSeconds = 60, keyType = "TOKEN")
    @PostMapping
    public ResponseEntity<?> createResume(@Valid @RequestBody ResumeRequest resume, Authentication authentication) {
        return resumService.create_Resume(resume, authentication);

    }

    @RateLimit(maxRequests = 8, timeWindowSeconds = 60, keyType = "TOKEN")
    @PreAuthorize("hasRole('HR')")
    @PutMapping("/hr/{id}/status")
    public ResponseEntity<?> updateResumeStatusHR(
            @PathVariable Long id, @Valid @RequestBody UpdateResumeHR updateResumeDTO, Authentication authentication) {
        return resumService.update_ResumeStatusHR(id, updateResumeDTO, authentication);

    }

    @RateLimit(maxRequests = 8, timeWindowSeconds = 60, keyType = "TOKEN")
    @PreAuthorize("hasRole('USER')")
    @PutMapping("/user/{id}")
    public ResponseEntity<?> updateResumeUser(
            @PathVariable Long id, @Valid @RequestBody UpdateResumeUser updateResumeDTO, Authentication authentication) {
        return resumService.update_ResumeUser(id, updateResumeDTO, authentication);

    }


    @RateLimit(maxRequests = 7, timeWindowSeconds = 60, keyType = "TOKEN")
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteResumebyId(@PathVariable Long id) {
        return resumService.delete_ResumebyId(id);
    }

    @RateLimit(maxRequests = 15, timeWindowSeconds = 60, keyType = "TOKEN")
    @GetMapping("/{id}")
    public ResponseEntity<?> detailResumebyId(@PathVariable Long id) {
        return resumService.detail_ResumebyId(id);
    }

    @RateLimit(maxRequests = 15, timeWindowSeconds = 60, keyType = "TOKEN")
    @GetMapping
    public ResponseEntity<?> GetallPageList(
            @RequestParam(value = "page") String pageparam, Authentication authentication) {
        return resumService.Getall_PageList(pageparam, authentication);

    }

    @RateLimit(maxRequests = 15, timeWindowSeconds = 60, keyType = "TOKEN")
    @GetMapping("/by-user")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<?> GetallResumebyUser(
            @RequestParam(value = "page") String pageparam, Authentication authentication) {
        return resumService.Getall_ResumebyUser(pageparam, authentication);
    }

    @RateLimit(maxRequests = 15, timeWindowSeconds = 60, keyType = "TOKEN")
    @PreAuthorize("hasRole('HR')")
    @GetMapping("/by-companyHR")
    public ResponseEntity<?> GetallResumeHRcompany(
            @RequestParam(value = "page") String pageparam, Authentication authentication) {

        return resumService.Getall_ResumeHRcompany(pageparam, authentication);
    }

    @RateLimit(maxRequests = 15, timeWindowSeconds = 60, keyType = "TOKEN")
    @GetMapping("/history-applied")
    public ResponseEntity<?> getResumeHistoryApplied(
            @RequestParam(defaultValue = "0") String page,
            @RequestParam(required = false) ResumeStatus status, Authentication authentication) {
        return resumService.get_ResumeHistoryApplied(page, status, authentication);
    }


}
