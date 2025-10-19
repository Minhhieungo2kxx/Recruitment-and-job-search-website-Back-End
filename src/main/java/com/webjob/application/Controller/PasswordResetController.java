package com.webjob.application.Controller;

import com.webjob.application.Annotation.RateLimit;
import com.webjob.application.Models.Entity.User;
import com.webjob.application.Models.Request.ForgotPasswordRequest;
import com.webjob.application.Models.Request.ResetPasswordRequest;
import com.webjob.application.Models.Response.ApiResponse;
import com.webjob.application.Services.PasswordResetService;
import com.webjob.application.Services.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/password")

public class PasswordResetController {
    private final PasswordResetService passwordResetService;

    private final UserService userService;

    public PasswordResetController(PasswordResetService passwordResetService, UserService userService) {
        this.passwordResetService = passwordResetService;
        this.userService = userService;
    }

    @RateLimit(maxRequests = 3, timeWindowSeconds = 300, keyType = "IP")
    @PostMapping("/forgot")
    public ResponseEntity<?> forgotPassword(@RequestBody @Valid ForgotPasswordRequest request) {
        User user=userService.getbyEmail(request.getEmail());
        passwordResetService.forgotPassword(user);
        ApiResponse<?>apiResponse=new ApiResponse<>(
                HttpStatus.OK.value(),null,
                "Password reset link has been sent to your email.",null);
        return ResponseEntity.ok(apiResponse);
    }

    @RateLimit(maxRequests = 5, timeWindowSeconds = 60, keyType = "IP")
    @PostMapping("/reset")
    public ResponseEntity<?> resetPassword(@RequestBody @Valid ResetPasswordRequest request) {
        passwordResetService.resetPassword(request);
        ApiResponse<?>apiResponse=new ApiResponse<>(
                HttpStatus.OK.value(),null,
                "Password has been successfully reset.",null);
        return ResponseEntity.ok(apiResponse);
    }
}
