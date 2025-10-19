package com.webjob.application.Controller;


import com.webjob.application.Annotation.RateLimit;
import com.webjob.application.Models.Request.Userrequest;
import com.webjob.application.Models.Response.ApiResponse;
import com.webjob.application.Models.Request.LoginDTO;
import com.webjob.application.Models.Response.LoginResponse;
import com.webjob.application.Models.Response.UserDTO;
import com.webjob.application.Models.Entity.User;
import com.webjob.application.Services.AuthService;
import com.webjob.application.Services.Redis.TokenBlacklistService;
import com.webjob.application.Services.SecurityUtil;
import com.webjob.application.Services.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth") // base path cho Auth
public class AuthController {

    @Value("${security.jwt.refresh-token-validity-in-seconds}")
    private Long jwtrefreshExpiration;
    private final AuthenticationManagerBuilder authenticationManagerBuilder;
    private final SecurityUtil securityUtil;

    private final UserService userService;

    private final JwtDecoder jwtDecoder;
    private final TokenBlacklistService tokenBlacklistService;

    private final AuthService authService;


    private final ModelMapper modelMapper;

    public AuthController(AuthenticationManagerBuilder authenticationManagerBuilder, SecurityUtil securityUtil, UserService userService, JwtDecoder jwtDecoder, TokenBlacklistService tokenBlacklistService, AuthService authService, ModelMapper modelMapper) {
        this.authenticationManagerBuilder = authenticationManagerBuilder;
        this.securityUtil = securityUtil;
        this.userService = userService;
        this.jwtDecoder = jwtDecoder;
        this.tokenBlacklistService = tokenBlacklistService;
        this.authService = authService;
        this.modelMapper = modelMapper;
    }

    @RateLimit(maxRequests = 5, timeWindowSeconds = 60, keyType = "IP")
    @PostMapping("/login")
    public ResponseEntity<?> formlogin(@Valid @RequestBody LoginDTO loginDTO) {
        return authService.handleLogin(loginDTO);
    }

    @RateLimit(maxRequests = 10, timeWindowSeconds = 60, keyType = "TOKEN")
    @GetMapping("/account")
    public ResponseEntity<?> getAccount() {
        return authService.getCurrentUserInfo();
    }

    @RateLimit(maxRequests = 10, timeWindowSeconds = 60, keyType = "IP")
    @GetMapping("/refresh")
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

