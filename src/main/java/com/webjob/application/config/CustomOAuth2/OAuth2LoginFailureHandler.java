package com.webjob.application.config.CustomOAuth2;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.webjob.application.dto.Response.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@RequiredArgsConstructor
@Component
public class OAuth2LoginFailureHandler implements AuthenticationFailureHandler {
    private final ObjectMapper objectMapper;
    @Override
    public void onAuthenticationFailure(HttpServletRequest request,
                                        HttpServletResponse response,
                                        AuthenticationException exception) throws IOException {

        ApiResponse<?> apiResponse = new ApiResponse<>();
        apiResponse.setStatusCode(HttpStatus.UNAUTHORIZED.value());
        apiResponse.setData(null);

        // ====== GỘP LOGIC TỪ CLASS 1 + CLASS 2 ======

        if (exception instanceof OAuth2AuthenticationException oauthEx) {

            String code = oauthEx.getError().getErrorCode();

            apiResponse.setMessage(mapErrorToMessage(code));
            apiResponse.setError(oauthEx.getMessage());

        } else {
            apiResponse.setMessage("authentication_failed");
            apiResponse.setError(exception.getMessage());
        }

        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);

        objectMapper.writeValue(response.getWriter(), apiResponse);
    }
    private String mapErrorToMessage(String code) {
        return switch (code) {
            case "access_denied" -> "User cancelled login";
            case "invalid_grant" -> "Invalid authorization code";
            case "invalid_token" -> "Invalid token";
            default -> "OAuth2 authentication failed";
        };
    }

}
