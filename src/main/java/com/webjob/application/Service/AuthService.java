package com.webjob.application.Service;

import com.webjob.application.Model.Entity.User;
import com.webjob.application.Dto.Request.LoginDTO;
import com.webjob.application.Dto.Request.Userrequest;
import com.webjob.application.Dto.Response.ApiResponse;
import com.webjob.application.Dto.Response.LoginResponse;
import com.webjob.application.Dto.Response.UserDTO;
import com.webjob.application.Service.Redis.TokenBlacklistService;
import jakarta.servlet.http.HttpServletRequest;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.stereotype.Service;

@Service
public class AuthService {
    private final AuthenticationManager authenticationManager;
    private final SecurityUtil securityUtil;
    private final UserService userService;
    private final ModelMapper modelMapper;

    @Value("${security.jwt.refresh-token-validity-in-seconds}")
    private Long jwtRefreshExpiration;
    private final JwtDecoder jwtDecoder;

    private final TokenBlacklistService tokenBlacklistService;

    public AuthService(AuthenticationManager authenticationManager, SecurityUtil securityUtil, UserService userService, ModelMapper modelMapper, JwtDecoder jwtDecoder, TokenBlacklistService tokenBlacklistService) {
        this.authenticationManager = authenticationManager;
        this.securityUtil = securityUtil;
        this.userService = userService;
        this.modelMapper = modelMapper;
        this.jwtDecoder = jwtDecoder;
        this.tokenBlacklistService = tokenBlacklistService;
    }


    public ResponseEntity<?> handleLogin(LoginDTO loginDTO) {
        Authentication authentication = authenticateUser(loginDTO);
        User user = getUserFromAuthentication(authentication);
        LoginResponse loginResponse = buildLoginResponse(user);
        ResponseCookie refreshCookie = createRefreshCookie(user);
        userService.updateRefreshtoken(user.getId(), refreshCookie.getValue());

        ApiResponse<LoginResponse> response = new ApiResponse<>(HttpStatus.OK.value(), null,
                "Call API Login successful",
                loginResponse
        );

        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.SET_COOKIE, refreshCookie.toString());

        return ResponseEntity.ok().headers(headers).body(response);
    }
    public ResponseEntity<?> getCurrentUserInfo() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (!isAuthenticated(authentication)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Unauthorized");
        }
        User user = getUserFromAuthentication(authentication);
        LoginResponse.User userDTO = modelMapper.map(user, LoginResponse.User.class);
        ApiResponse<?> response = new ApiResponse<>(HttpStatus.OK.value(), null,
                "Get Account successful",
                userDTO
        );
        return ResponseEntity.ok(response);
    }
    public ResponseEntity<?> refreshToken(String refreshToken) {
        if ("default".equals(refreshToken)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ApiResponse<>(HttpStatus.UNAUTHORIZED.value(), "No refresh token found.", null, null));
        }
        Jwt decodedJwt;
        try {
            decodedJwt = jwtDecoder.decode(refreshToken);
        } catch (JwtException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ApiResponse<>(HttpStatus.UNAUTHORIZED.value(), "Invalid refresh token.", null, null));
        }
        String email = decodedJwt.getSubject();
        User user = userService.getEmailAndRefreshtoken(email, refreshToken);
        LoginResponse loginResponse = buildLoginResponse(user);
        ResponseCookie refreshCookie = createRefreshCookie(user);
        // cập nhật refresh token mới vào DB
        userService.updateRefreshtoken(user.getId(),refreshCookie.getValue());
        // đóng gói response
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.SET_COOKIE, refreshCookie.toString());
        ApiResponse<?> response = new ApiResponse<>(HttpStatus.OK.value(), null,
                "Refresh token successful", loginResponse
        );
        return ResponseEntity.ok().headers(headers).body(response);
    }
    public ResponseEntity<?> logout(HttpServletRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (!isAuthenticated(authentication)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Unauthorized");
        }
        String token = extractBearerToken(request);
        if (token != null) {
            blacklistToken(token);
        }
        User user = getUserFromAuthentication(authentication);
        clearRefreshToken(user);
        HttpHeaders headers = clearRefreshCookie();
        ApiResponse<Object> response = new ApiResponse<>(
                HttpStatus.OK.value(),
                null,
                "Logout User Success",
                null
        );
        return ResponseEntity.ok().headers(headers).body(response);
    }
    public ResponseEntity<?> register(Userrequest userrequest) {

        User user = modelMapper.map(userrequest, User.class);

        User savedUser = userService.handle(user);

        UserDTO userDTO = modelMapper.map(savedUser, UserDTO.class);
        // Tạo response
        ApiResponse<UserDTO> response = new ApiResponse<>(
                HttpStatus.CREATED.value(),
                null,
                "Register Account successful",
                userDTO
        );
        return new ResponseEntity<>(response, HttpStatus.CREATED);
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

    private HttpHeaders clearRefreshCookie() {
        ResponseCookie deleteCookie = ResponseCookie.from("refresh", "")
                .httpOnly(true).secure(true).path("/").maxAge(0).build();
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.SET_COOKIE, deleteCookie.toString());
        return headers;
    }



    private Authentication authenticateUser(LoginDTO loginDTO) {
        UsernamePasswordAuthenticationToken authenticationToken =
                new UsernamePasswordAuthenticationToken(loginDTO.getUsername(), loginDTO.getPassword());

        Authentication authentication = authenticationManager.authenticate(authenticationToken);
        SecurityContextHolder.getContext().setAuthentication(authentication);
        return authentication;
    }

    private User getUserFromAuthentication(Authentication authentication) {
        String email = authentication.getName();
        return userService.getbyEmail(email);
    }

    private LoginResponse buildLoginResponse(User user) {
        LoginResponse.User userDTO = modelMapper.map(user, LoginResponse.User.class);
        String accessToken = securityUtil.createacessToken(user.getEmail(), userDTO);
        String refreshToken = securityUtil.createrefreshToken(user.getEmail(), userDTO);
        return new LoginResponse(accessToken, userDTO);
    }

    private ResponseCookie createRefreshCookie(User user) {
        LoginResponse.User userDTO = modelMapper.map(user, LoginResponse.User.class);
        String refreshToken = securityUtil.createrefreshToken(user.getEmail(), userDTO);

        return ResponseCookie.from("refresh", refreshToken)
                .httpOnly(true)
                .secure(true)
                .path("/")
                .maxAge(jwtRefreshExpiration)
                .build();
    }


}
