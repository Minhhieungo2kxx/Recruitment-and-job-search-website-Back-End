package com.webjob.application.Configs;

import com.webjob.application.Models.Permission;
import com.webjob.application.Models.Role;
import com.webjob.application.Models.User;
import com.webjob.application.Services.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import org.springframework.web.servlet.HandlerExceptionResolver;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.HandlerMapping;


import java.util.List;
import java.util.Objects;


public class PermissionInterceptor implements HandlerInterceptor {
    @Autowired
    private UserService userService;

    @Autowired
    @Qualifier("handlerExceptionResolver")
    private HandlerExceptionResolver exceptionResolver;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        try {
            String path = (String) request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);
            String requestURI = request.getRequestURI();
            String httpMethod = request.getMethod();

            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated()) {
                throw new IllegalArgumentException("User is not authenticated");
            }

            User user = userService.getbyEmail(authentication.getName());
            if (user == null) {
                throw new IllegalArgumentException("User not found");
            }

            Role role = user.getRole();
            if (role == null) {
                throw new IllegalArgumentException("No role assigned");
            }

            List<Permission> permissions = role.getPermissions();
            if (permissions == null || permissions.isEmpty()) {
                throw new IllegalArgumentException("No permissions assigned to role");
            }

            boolean hasPermission = permissions.stream()
                    .anyMatch(p -> Objects.equals(p.getApiPath(), path) &&
                            Objects.equals(p.getMethod(), httpMethod));

            if (!hasPermission) {
                throw new IllegalArgumentException("You do not have permission to access this resource");
            }

            return true;
        } catch (IllegalArgumentException e) {
            exceptionResolver.resolveException(request, response, handler, e);
            return false;
        }
    }
}





