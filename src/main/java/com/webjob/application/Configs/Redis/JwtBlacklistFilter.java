package com.webjob.application.Configs.Redis;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.webjob.application.Models.Response.ApiResponse;
import com.webjob.application.Services.Redis.TokenBlacklistService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JwtBlacklistFilter extends OncePerRequestFilter {
    private final TokenBlacklistService tokenBlacklistService;
    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    public JwtBlacklistFilter(TokenBlacklistService tokenBlacklistService) {
        this.tokenBlacklistService = tokenBlacklistService;
    }
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String header = request.getHeader("Authorization");


        if (header != null && header.startsWith("Bearer ")){
            String token = header.substring(7);
            // Kiểm tra token có nằm trong blacklist không
            if (tokenBlacklistService.isTokenBlacklisted(token)) {
                logger.warn("Token has been blacklisted: "+token.substring(0, 6) + "*****");

                ApiResponse<?> apiResponse = new ApiResponse<>(
                        HttpStatus.UNAUTHORIZED.value(),
                        "Access token is invalidated",
                        "Exception Handle",
                        null
                );
                response.setStatus(HttpStatus.UNAUTHORIZED.value());
                response.setContentType(MediaType.APPLICATION_JSON_VALUE);

                try {
                    objectMapper.writeValue(response.getWriter(), apiResponse);
                } catch (IOException e) {
                    logger.error("Error writing the response", e);
                    response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
                    response.getWriter().write("Internal Server Error");
                }
                return;
            }

        }



        // Tiếp tục xử lý các filter khác
        filterChain.doFilter(request, response);
    }

    // Phương thức xử lý lỗi tái sử dụng




}

