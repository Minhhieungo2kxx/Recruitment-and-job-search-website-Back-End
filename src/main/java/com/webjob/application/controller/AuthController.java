package com.webjob.application.controller;


import com.webjob.application.annotation.RateLimit;
import com.webjob.application.dto.Request.Userrequest;
import com.webjob.application.dto.Request.LoginDTO;
import com.webjob.application.dto.Response.ApiResponse;
import com.webjob.application.dto.Response.LoginResponse;
import com.webjob.application.dto.Response.UserDTO;
import com.webjob.application.service.AuthService;


import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.http.*;


import org.springframework.web.bind.annotation.*;
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/auth") // base path cho Auth
public class AuthController {

    private final AuthService authService;

    @RateLimit(maxRequests = 5, timeWindowSeconds = 60, keyType = "IP")
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@Valid @RequestBody LoginDTO loginDTO
            ,HttpServletRequest request) {
        LoginResponse loginResponse=authService.handleLogin(loginDTO,request);
        ApiResponse<LoginResponse> response = new ApiResponse<>(
                HttpStatus.OK.value(),
                null,
                "Call API Login successful",
                loginResponse

        );
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.SET_COOKIE,loginResponse.getRefreshCookie());
        return ResponseEntity.ok().headers(headers).body(response);
    }


    @RateLimit(maxRequests = 10, timeWindowSeconds = 60, keyType = "TOKEN")
    @GetMapping("/account")
    public ResponseEntity<ApiResponse<LoginResponse.User>> getAccountInfo() { // Hoặc getCurrentUser()
        ApiResponse<LoginResponse.User> response = new ApiResponse<>(
                HttpStatus.OK.value()
                , null,
                "Get Account successful",
                authService.getCurrentUserInfo()

        );
        return ResponseEntity.ok(response);
    }

    @RateLimit(maxRequests = 10, timeWindowSeconds = 60, keyType = "IP")
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<LoginResponse>> refreshToken(
            @CookieValue(name = "refresh", defaultValue = "default") String refreshToken) {
        LoginResponse loginResponse=authService.refreshToken(refreshToken);
        ApiResponse<LoginResponse> response = new ApiResponse<>(
                200,
                null,
                "Refresh token successful",
                loginResponse
        );
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.SET_COOKIE, loginResponse.getRefreshCookie());
        return ResponseEntity.ok().headers(headers).body(response);
    }


    @RateLimit(maxRequests = 5, timeWindowSeconds = 60, keyType = "TOKEN")
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Object>> logout(HttpServletRequest request) {
        authService.logout(request);
        HttpHeaders headers = authService.clearRefreshCookie();
        ApiResponse<Object> response = new ApiResponse<>(
                HttpStatus.OK.value(),
                null,
                "Logout User Success",
                null
        );
        return ResponseEntity.ok().headers(headers).body(response);
    }


    @RateLimit(maxRequests = 5, timeWindowSeconds = 60, keyType = "IP")
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<UserDTO>> createRegister(@Valid @RequestBody Userrequest userrequest) {

        ApiResponse<UserDTO> response = new ApiResponse<>(
                HttpStatus.CREATED.value(),
                null,
                "Register Account successful",
                authService.register(userrequest)
        );
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

}

