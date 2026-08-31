package com.webjob.application.config.CustomOAuth2;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.webjob.application.models.Entity.User;
import com.webjob.application.dto.Response.LoginResponse;
import com.webjob.application.service.Redis.LoginNotificationService;
import com.webjob.application.service.SendEmail.ApplicationEmailService;
import com.webjob.application.service.UserService;
import com.webjob.application.utils.common.SecurityUtils;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
@Slf4j
public class OAuth2LoginSuccessHandler implements AuthenticationSuccessHandler {


    private final UserService userService;

    private final ModelMapper modelMapper;
    @Value("${security.jwt.refresh-token-validity-in-seconds}")
    private Long jwtrefreshExpiration;

    private final ObjectMapper objectMapper;
    private final ApplicationEmailService applicationEmailService;

    private final LoginNotificationService loginNotificationService;
    private final SecurityUtils securityUtils;



    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {

        try {

            DefaultOAuth2User oauthUser = (DefaultOAuth2User) authentication.getPrincipal();
            Map<String, Object> attributes = oauthUser.getAttributes();
            String email = oauthUser.getAttribute("email");
            String userId = oauthUser.getAttribute("userId"); //ở CustomOAuth2UserService
            if (userId == null) {
                response.sendRedirect("/login-chat?error=user_not_found");
                return;
            }
            User userEntity = userService.getById(Long.valueOf(userId));
            if (userEntity == null) {
                response.sendRedirect("/login-chat?error=user_not_found");
                return;
            }
            LoginResponse.User userDto = modelMapper.map(userEntity, LoginResponse.User.class);
            String accessToken = securityUtils.createacessToken(userEntity);

            String refreshToken = securityUtils.createrefreshToken(userEntity);
            String refreshTokenHash = securityUtils.sha256(refreshToken);
            userService.updateRefreshtoken(userEntity.getId(), refreshTokenHash);
            ResponseCookie cookie = ResponseCookie.from("refresh", refreshToken)
                    .httpOnly(true)
                    .secure(true)
                    .path("/")
                    .maxAge(jwtrefreshExpiration)
                    .build();
            response.setHeader(HttpHeaders.SET_COOKIE, cookie.toString());

//            send email
            handleLoginNotificationEmail(email,Long.valueOf(userId),request);

            String redirectUrl = String.format("/login-success?accessToken=%s&userInfo=%s",
                    accessToken,
                    URLEncoder.encode(objectMapper.writeValueAsString(userDto), StandardCharsets.UTF_8));
            response.sendRedirect(redirectUrl);

        } catch (Exception e) {
            log.error("OAuth2 authentication success handler failed", e);
            response.sendRedirect("/login-chat?error=oauth_failed");
        }
    }
    public void LoginNotificationEvent(HttpServletRequest request, String email) {

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
    private void handleLoginNotificationEmail(String email, Long userId,HttpServletRequest request) {
        if (email == null || email.isBlank()) {
            return;
        }
        if (!loginNotificationService.shouldSendLoginNotification(userId)) {
            return;
        }
        try {
            LoginNotificationEvent(request,email);

        } catch (Exception e) {
            log.error("Cannot send login notification. userId={}", userId, e);
            loginNotificationService.removeNotificationFlag(userId);
        }
    }


}

