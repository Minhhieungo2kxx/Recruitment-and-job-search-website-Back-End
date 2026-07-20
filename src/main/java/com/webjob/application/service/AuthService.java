package com.webjob.application.service;

import com.webjob.application.dto.record.LoginSuccessEvent;
import com.webjob.application.exception.Customs.UnauthorizedException;
import com.webjob.application.models.Entity.User;
import com.webjob.application.dto.Request.LoginDTO;
import com.webjob.application.dto.Request.Userrequest;
import com.webjob.application.dto.Response.ApiResponse;
import com.webjob.application.dto.Response.LoginResponse;
import com.webjob.application.dto.Response.UserDTO;
import com.webjob.application.service.Redis.TokenBlacklistService;
import com.webjob.application.service.SendEmail.ApplicationEmailService;
import com.webjob.application.utils.common.SecurityUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class AuthService {
    private final AuthenticationManager authenticationManager;
    private final SecurityUtil securityUtil;
    private final UserService userService;
    private final ModelMapper modelMapper;

    private final SecurityUtils securityUtils;

    @Value("${security.jwt.refresh-token-validity-in-seconds}")
    private Long jwtRefreshExpiration;
    private final JwtDecoder jwtDecoder;

    private final TokenBlacklistService tokenBlacklistService;

    private final ApplicationEmailService applicationEmailService;
    private final ApplicationEventPublisher eventPublisher;


    @Transactional
    public LoginResponse handleLogin(LoginDTO loginDTO, HttpServletRequest request) {
        Authentication authentication = authenticateUser(loginDTO);
        User user = getUserFromAuthentication(authentication);
        LoginResponse loginResponse = buildLoginResponse(user);
        ResponseCookie refreshCookie = createRefreshCookie(user);
        userService.updateRefreshtoken(user.getId(), refreshCookie.getValue());
        loginResponse.setRefreshCookie(refreshCookie.toString());
        //send Email
        eventPublisher.publishEvent(
                new LoginSuccessEvent(
                        user.getEmail(),
                        getClientIp(request),
                        request.getHeader("User-Agent"),
                        LocalDateTime.now()
                )
        );
       return loginResponse;
    }

    public LoginResponse.User getCurrentUserInfo() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        User user = getUserFromAuthentication(authentication);
        LoginResponse.User userDTO = modelMapper.map(user, LoginResponse.User.class);
        return userDTO;
//
    }

    @Transactional
    public LoginResponse refreshToken(String refreshToken) {

        // 1. Nếu không có token → reject
        if ("default".equals(refreshToken) || refreshToken == null) {
            throw new UnauthorizedException("No refresh token found");
        }

        // 2. Lấy user theo refresh token từ DB
        User user = null;
        try {
            user = userService.getUserByRefreshToken(refreshToken);
        } catch (UsernameNotFoundException ex) {
            throw new UnauthorizedException("Refresh token expired or revoked");
        }

        // 3. Decode JWT chỉ để lấy email (không dùng để kiểm tra validity!)
        Jwt decodedJwt;
        try {
            decodedJwt = jwtDecoder.decode(refreshToken);
        } catch (JwtException e) {
            throw new UnauthorizedException("Invalid refresh token.");
        }

        // 4. Kiểm tra email từ JWT có khớp user DB
        if (!decodedJwt.getClaim("email").equals(user.getEmail())) {
            throw new UnauthorizedException("Refresh token does not match user");
        }
        // 5. Build login response (tạo access token mới)
        LoginResponse loginResponse = buildLoginResponse(user);

        // 6. Tạo refresh token mới
        ResponseCookie newRefreshCookie = createRefreshCookie(user);

        // 7. Cập nhật refresh token mới vào DB (revoke token cũ)
        userService.updateRefreshtoken(user.getId(), newRefreshCookie.getValue());

        loginResponse.setRefreshCookie(newRefreshCookie.toString());
        return loginResponse;

    }


@Transactional
public void logout(HttpServletRequest request) {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (!isAuthenticated(authentication)) {
        throw new UnauthorizedException("Unauthorized");
    }
    String token = extractBearerToken(request);
    if (token != null) {
        blacklistToken(token);
    }
    User user = getUserFromAuthentication(authentication);
    clearRefreshToken(user);

}


    @Transactional
    public UserDTO register(Userrequest userrequest) {
        User savedUser = userService.registerClientUser(userrequest);
        UserDTO userDTO = modelMapper.map(savedUser, UserDTO.class);
        // Tạo response
        return userDTO;
    }


    private boolean isAuthenticated(Authentication auth) {
        return auth != null && auth.isAuthenticated() && !(auth instanceof AnonymousAuthenticationToken);
    }

    public String extractBearerToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            return header.substring(7);
        }
        return null;
    }

    private void blacklistToken(String token) {
        long remaining = securityUtil.getRemainingValidity(token);
        tokenBlacklistService.blacklistToken(token, remaining);
    }

    private void clearRefreshToken(User user) {
        userService.updateRefreshtoken(user.getId(), null);
    }

    public HttpHeaders clearRefreshCookie() {
        ResponseCookie deleteCookie = ResponseCookie.from("refresh", "")
                .httpOnly(true)
                .secure(true)
                .path("/")
                .domain("localhost") // PHẢI GIỐNG LOGIN
                .maxAge(0)
                .build();

        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.SET_COOKIE, deleteCookie.toString());
        return headers;
    }




    private Authentication authenticateUser(LoginDTO loginDTO) {
        UsernamePasswordAuthenticationToken authenticationToken =
                new UsernamePasswordAuthenticationToken(loginDTO.getUsername(), loginDTO.getPassword());

        Authentication authentication = authenticationManager.authenticate(authenticationToken);
        SecurityContextHolder.getContext().setAuthentication(authentication);
        return authentication;
    }

    public User getUserFromAuthentication(Authentication authentication) {
        Long userId = Long.parseLong(authentication.getName());

        User user = userService.getById(userId);

        switch (user.getStatus()) {
            case ACTIVE:
                return user;

            case BLOCKED:
                throw new UnauthorizedException("Your account has been blocked.");

            case PENDING:
                throw new UnauthorizedException("Your account is pending verification.");

            case INACTIVE:
                throw new UnauthorizedException("Your account has been deactivated.");

            default:
                throw new UnauthorizedException("Invalid account status.");
        }
    }

    private LoginResponse buildLoginResponse(User user) {
        LoginResponse.User userDTO = modelMapper.map(user, LoginResponse.User.class);
        String accessToken = securityUtil.createacessToken(user);
        return LoginResponse.builder()
                .accessToken(accessToken)
                .user(userDTO)
                .build();
    }

    private ResponseCookie createRefreshCookie(User user) {
        LoginResponse.User userDTO = modelMapper.map(user, LoginResponse.User.class);
        String refreshToken = securityUtil.createrefreshToken(user);

        return ResponseCookie.from("refresh", refreshToken)
                .httpOnly(true)
                .secure(true)
                .path("/")
                .domain("localhost") //  BẮT BUỘC
                .maxAge(jwtRefreshExpiration)
                .build();
    }


    public String getClientIp(HttpServletRequest request) {
        String[] headers = {
                "X-Forwarded-For",
                "Proxy-Client-IP",
                "WL-Proxy-Client-IP",
                "HTTP_X_FORWARDED_FOR",
                "HTTP_X_FORWARDED",
                "HTTP_X_CLUSTER_CLIENT_IP",
                "HTTP_CLIENT_IP",
                "HTTP_FORWARDED_FOR",
                "HTTP_FORWARDED"
        };

        for (String header : headers) {
            String ip = request.getHeader(header);
            if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
                return ip.split(",")[0].trim();
            }
        }
        return request.getRemoteAddr();
    }


}
