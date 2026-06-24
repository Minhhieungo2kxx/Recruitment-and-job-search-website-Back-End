package com.webjob.application.controller;


import com.webjob.application.annotation.RateLimit;
import com.webjob.application.dto.Request.Userrequest;
import com.webjob.application.dto.Request.LoginDTO;
import com.webjob.application.service.AuthService;
import com.webjob.application.service.Redis.TokenBlacklistService;


import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;

import org.springframework.http.*;


import org.springframework.web.bind.annotation.*;
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/auth") // base path cho Auth
public class AuthController {

    private final AuthService authService;

    @RateLimit(maxRequests = 5, timeWindowSeconds = 60, keyType = "IP")
    @PostMapping("/login")
    public ResponseEntity<?> formlogin(@Valid @RequestBody LoginDTO loginDTO,HttpServletRequest request) {
        return authService.handleLogin(loginDTO,request);
    }

    @RateLimit(maxRequests = 10, timeWindowSeconds = 60, keyType = "TOKEN")
    @GetMapping("/account")
    public ResponseEntity<?> getAccount() {
        return authService.getCurrentUserInfo();
    }

    @RateLimit(maxRequests = 10, timeWindowSeconds = 60, keyType = "IP")
    @PostMapping("/refresh")
    public ResponseEntity<?> getRefreshToken(@CookieValue(name = "refresh", defaultValue = "default") String refreshToken) {
        return authService.refreshToken(refreshToken);

    }

    @RateLimit(maxRequests = 5, timeWindowSeconds = 60, keyType = "TOKEN")
    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletRequest request) {
        return authService.logout(request);
    }


    @RateLimit(maxRequests = 5, timeWindowSeconds = 60, keyType = "IP")
    @PostMapping("/register")
    public ResponseEntity<?> createRegister(@Valid @RequestBody Userrequest userrequest) {
        return authService.register(userrequest);
    }

}

