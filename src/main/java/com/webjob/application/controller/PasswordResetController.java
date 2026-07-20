package com.webjob.application.controller;

import com.webjob.application.annotation.RateLimit;
import com.webjob.application.dto.Request.ForgotPasswordRequest;
import com.webjob.application.dto.Request.ResetPasswordRequest;
import com.webjob.application.dto.Response.ApiResponse;
import com.webjob.application.service.PasswordResetService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/password")
@RequiredArgsConstructor
public class PasswordResetController {
    private final PasswordResetService passwordResetService;


    @RateLimit(maxRequests = 3, timeWindowSeconds = 300, keyType = "IP")
    @PostMapping("/forgot")
    public ResponseEntity<ApiResponse<Object>> forgotPassword(@RequestBody @Valid ForgotPasswordRequest request) {
        passwordResetService.forgotPassword(request);
        ApiResponse<Object> apiResponse = new ApiResponse<>(
                HttpStatus.OK.value(),
                null,
                "Password reset link has been sent to your email.",
                null);
        return ResponseEntity.ok(apiResponse);
    }

    @RateLimit(maxRequests = 5, timeWindowSeconds = 60, keyType = "IP")
    @PostMapping("/reset")
    public ResponseEntity<ApiResponse<Object>> resetPassword(@RequestBody @Valid ResetPasswordRequest request) {
        passwordResetService.resetPassword(request);
        ApiResponse<Object> apiResponse = new ApiResponse<>(
                HttpStatus.OK.value(),
                null,
                "Password has been successfully reset.",
                null);
        return ResponseEntity.ok(apiResponse);
    }
}
