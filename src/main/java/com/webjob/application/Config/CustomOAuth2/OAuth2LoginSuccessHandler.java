package com.webjob.application.Config.CustomOAuth2;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.webjob.application.Model.Entity.User;
import com.webjob.application.Dto.Response.LoginResponse;
import com.webjob.application.Service.SecurityUtil;
import com.webjob.application.Service.UserService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Component
public class OAuth2LoginSuccessHandler implements AuthenticationSuccessHandler {

    private final SecurityUtil securityUtil;

    private final UserService userService;
    @Autowired
    private ModelMapper modelMapper;
    @Value("${security.jwt.refresh-token-validity-in-seconds}")
    private Long jwtrefreshExpiration;

    @Autowired
    private ObjectMapper objectMapper;

    public OAuth2LoginSuccessHandler(SecurityUtil securityUtil, UserService userService) {
        this.securityUtil = securityUtil;
        this.userService = userService;
    }


    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {

        try {
            // Lấy thông tin user từ Google OAuth2
            String email = ((DefaultOAuth2User) authentication.getPrincipal()).getAttribute("email");
            User userEntity = userService.getEmailbyGoogle(email);

            // Tạo JWT tokens
            LoginResponse.User userDto = modelMapper.map(userEntity, LoginResponse.User.class);
            String accessToken = securityUtil.createacessToken(userEntity.getEmail(), userDto);
            String refreshToken = securityUtil.createrefreshToken(userEntity.getEmail(), userDto);

            // Cập nhật refresh token vào database
            userService.updateRefreshtoken(userEntity.getId(), refreshToken);

            // Gửi refresh token qua cookie
            ResponseCookie cookie = ResponseCookie.from("refresh", refreshToken)
                    .httpOnly(true)
                    .secure(true)
                    .path("/")
                    .maxAge(jwtrefreshExpiration)
                    .build();
            response.setHeader(HttpHeaders.SET_COOKIE, cookie.toString());

            // Chuyển hướng đến trang success với token trong URL parameters
            String redirectUrl = String.format("/login-success?accessToken=%s&userInfo=%s",
                    accessToken,
                    URLEncoder.encode(objectMapper.writeValueAsString(userDto), StandardCharsets.UTF_8));

            response.sendRedirect(redirectUrl);

        } catch (Exception e) {
            // Nếu có lỗi, chuyển hướng đến trang login với thông báo lỗi
            response.sendRedirect("/login-chat?error=oauth_failed");
        }
    }


}

