package com.webjob.application.Controller;


import com.webjob.application.Models.Response.ApiResponse;
import com.webjob.application.Models.Request.LoginDTO;
import com.webjob.application.Models.Response.LoginResponse;
import com.webjob.application.Models.User;
import com.webjob.application.Services.SecurityUtil;
import com.webjob.application.Services.UserService;
import jakarta.validation.Valid;
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
public class AuthController {

    @Value("${security.jwt.refresh-token-validity-in-seconds}")
    private Long jwtrefreshExpiration;
    private final AuthenticationManagerBuilder authenticationManagerBuilder;
    private final SecurityUtil securityUtil;

    private final UserService userService;

    private final JwtDecoder jwtDecoder;

    public AuthController(AuthenticationManagerBuilder authenticationManagerBuilder, SecurityUtil securityUtil, UserService userService, JwtDecoder jwtDecoder) {
        this.authenticationManagerBuilder = authenticationManagerBuilder;
        this.securityUtil = securityUtil;
        this.userService = userService;
        this.jwtDecoder = jwtDecoder;
    }




    @PostMapping("/auth/login")
    public ResponseEntity<?> formlogin(@Valid @RequestBody LoginDTO loginDTO) {
        UsernamePasswordAuthenticationToken authenticationToken =
                new UsernamePasswordAuthenticationToken(loginDTO.getUsername(), loginDTO.getPassword());

        Authentication authentication = authenticationManagerBuilder.getObject().authenticate(authenticationToken);
//        set thong tin authencation thanh cong
        SecurityContextHolder.getContext().setAuthentication(authentication);
//        get authentication thanh cong
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String getEmail=auth.getName();
        User respon=userService.getbyEmail(getEmail);

        LoginResponse.User user=new LoginResponse.User(respon.getId(),respon.getEmail(),respon.getFullName());
        String acess_token=securityUtil.createacessToken(respon.getEmail(),user);
        LoginResponse loginResponse=new LoginResponse(acess_token,user);
        String refresh_token=securityUtil.createrefreshToken(respon.getEmail(),loginResponse);
        userService.updateRefreshtoken(respon.getId(),refresh_token);
        ResponseCookie responseCookie= ResponseCookie.from("refresh",refresh_token)
                .httpOnly(true)
                .secure(true)
                .path("/")
                .maxAge(jwtrefreshExpiration)
                .build();
        HttpHeaders httpHeaders=new HttpHeaders();
        httpHeaders.set(HttpHeaders.SET_COOKIE,responseCookie.toString());
        ApiResponse<LoginResponse> response = new ApiResponse<>(
                HttpStatus.OK.value(),
                null,
                "Call API Login successful",
                loginResponse
        );

        return ResponseEntity.ok().headers(httpHeaders).body(response);

    }
//    Lay ra thong tin nguoi dung khi da gui kem access-token khi yeu cau den server
//    server se giai ma token do roi phan hoi lai client-->ta co authencaition nen get duoc email
    @GetMapping("/auth/account")
    public ResponseEntity<?> getAccount() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        // Kiểm tra xác thực có hợp lệ không
        if (authentication == null || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Unauthorized");
        }

        String email = authentication.getName();
        User user = userService.getbyEmail(email);
        LoginResponse.User responseUser = new LoginResponse.User(user.getId(), user.getEmail(),user.getFullName());
        ApiResponse<LoginResponse.User> response = new ApiResponse<>(
                HttpStatus.OK.value(),
                null,
                "get Account successful",
                responseUser
        );
        return ResponseEntity.ok(response);
    }
    @GetMapping("/auth/refresh")
    public ResponseEntity<?> getRefreshToken(@CookieValue(name = "refresh", defaultValue = "default") String refreshToken) {
        if ("default".equals(refreshToken)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("No refresh token found.");
        }
            Jwt decodedJwt = jwtDecoder.decode(refreshToken);
            String username = decodedJwt.getSubject();
            User getbyuser=userService.getEmailAndRefreshtoken(username,refreshToken);

            LoginResponse.User user=new LoginResponse.User(getbyuser.getId(),getbyuser.getEmail(),getbyuser.getFullName());
            String acess_token=securityUtil.createacessToken(getbyuser.getEmail(),user);
            LoginResponse loginResponse=new LoginResponse(acess_token,user);
            String refresh_token=securityUtil.createrefreshToken(getbyuser.getEmail(),loginResponse);
            userService.updateRefreshtoken(getbyuser.getId(),refresh_token);
            ResponseCookie responseCookie= ResponseCookie.from("refresh",refresh_token)
                    .httpOnly(true).secure(true).path("/").maxAge(jwtrefreshExpiration).build();
            HttpHeaders httpHeaders=new HttpHeaders();
            httpHeaders.set(HttpHeaders.SET_COOKIE,responseCookie.toString());
            ApiResponse<LoginResponse> response = new ApiResponse<>(
                HttpStatus.OK.value(),
                null,
                "Get User by refresh token",
                loginResponse
        );
            return ResponseEntity.ok().headers(httpHeaders).body(response);

    }
    @PostMapping("/auth/logout")
    public ResponseEntity<?> logout() {
        // Lấy thông tin xác thực hiện tại từ context
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        // Kiểm tra xác thực có hợp lệ không
        if (authentication == null || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Unauthorized");
        }
        // Lấy email từ token
        String email = authentication.getName();
        User user = userService.getbyEmail(email);
        // Xoá refresh_token trong DB (set null)
        userService.updateRefreshtoken(user.getId(),null); // cần thêm method này nếu chưa có
        // Xoá cookie "refresh" bằng cách đặt maxAge = 0
        ResponseCookie deleteCookie = ResponseCookie.from("refresh", "")
                .httpOnly(true).secure(true).path("/").maxAge(0).build();
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.SET_COOKIE, deleteCookie.toString());
        ApiResponse<Object> response = new ApiResponse<>(
                HttpStatus.OK.value(),
                null,
                "Logout User",
                null
        );

        return ResponseEntity.ok().headers(headers).body(response);
    }

}

