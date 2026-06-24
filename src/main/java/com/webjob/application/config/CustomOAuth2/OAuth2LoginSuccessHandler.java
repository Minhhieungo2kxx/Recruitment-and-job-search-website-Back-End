package com.webjob.application.config.CustomOAuth2;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.webjob.application.models.Entity.User;
import com.webjob.application.dto.Response.LoginResponse;
import com.webjob.application.service.AuthService;
import com.webjob.application.service.SecurityUtil;
import com.webjob.application.service.SendEmail.ApplicationEmailService;
import com.webjob.application.service.UserService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
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
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

@RequiredArgsConstructor
@Component
public class OAuth2LoginSuccessHandler implements AuthenticationSuccessHandler {

    private final SecurityUtil securityUtil;

    private final UserService userService;

    private final ModelMapper modelMapper;
    @Value("${security.jwt.refresh-token-validity-in-seconds}")
    private Long jwtrefreshExpiration;

    private final ObjectMapper objectMapper;
    private final ApplicationEmailService applicationEmailService;



    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {

        try {
            // Lấy thông tin user từ Google OAuth2
            String email = ((DefaultOAuth2User) authentication.getPrincipal()).getAttribute("email");
            User userEntity = userService.getbyEmail(email);

            // Tạo JWT tokens
            LoginResponse.User userDto = modelMapper.map(userEntity, LoginResponse.User.class);
            String accessToken = securityUtil.createacessToken(userEntity);
            String refreshToken = securityUtil.createrefreshToken(userEntity);
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
            //send Email

            handleLoginNotification(request,email);

        } catch (Exception e) {
            // Nếu có lỗi, chuyển hướng đến trang login với thông báo lỗi
            response.sendRedirect("/login-chat?error=oauth_failed");
        }
    }
    public void handleLoginNotification(HttpServletRequest request, String email) {
        // Trong Controller hoặc nơi gọi async
        String ip = getClientIp(request);
        String userAgent = request.getHeader("User-Agent");
        Map<String, Object> emailVars = new HashMap<>();
        emailVars.put("email", email);
        emailVars.put("time", LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")));
        emailVars.put("ip", ip);
        emailVars.put("userAgent", userAgent);
        applicationEmailService.LoginNotification(emailVars);
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

