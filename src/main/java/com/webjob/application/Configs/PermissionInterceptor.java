package com.webjob.application.Configs;

import com.webjob.application.Models.Entity.Permission;
import com.webjob.application.Models.Entity.Role;
import com.webjob.application.Models.Entity.User;
import com.webjob.application.Services.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import org.springframework.web.servlet.HandlerExceptionResolver;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.HandlerMapping;


import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;


public class PermissionInterceptor implements HandlerInterceptor {
    @Autowired
    private UserService userService;

    @Autowired
    @Qualifier("handlerExceptionResolver")
    private HandlerExceptionResolver exceptionResolver;

    private static final Logger logger = LoggerFactory.getLogger(PermissionInterceptor.class);

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        try {
            String path = (String) request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);
            String requestURI = request.getRequestURI();
            String httpMethod = request.getMethod();
            logger.info("Incoming request: URI = {}", requestURI);
            logger.info("Method = {}",httpMethod);
            logger.info("Path = {}",path);

            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated()) {
                logger.warn("Unauthenticated access attempt");
                throw new IllegalArgumentException("User is not authenticated");
            }
            String userEmail = authentication.getName();
            logger.info("Authenticated user: {}", userEmail);

            User user = userService.getbyEmail(authentication.getName());
            if (user == null) {
                logger.error("User not found: {}", userEmail);
                throw new IllegalArgumentException("User not found");
            }

            Role role = user.getRole();
            if (role == null) {
                logger.error("No role assigned to user: {}", userEmail);
                throw new IllegalArgumentException("No role assigned");
            }

            List<Permission> permissions = role.getPermissions();
            if (permissions == null || permissions.isEmpty()) {
                logger.warn("No permissions assigned to role: {}", role.getName());
                throw new IllegalArgumentException("No permissions assigned to role");
            }
            logger.debug("User {} has permissions: {}", userEmail,
                    permissions.stream()
                            .map(p -> "[" + p.getMethod() + " " + p.getApiPath() + "]")
                            .collect(Collectors.joining(", "))
            );

            boolean hasPermission = permissions.stream()
                    .anyMatch(p -> Objects.equals(p.getApiPath(), path) &&
                            Objects.equals(p.getMethod(), httpMethod));

            if (!hasPermission) {
                logger.warn("User {} does not have permission to access {} {}", userEmail, httpMethod, path);
                throw new IllegalArgumentException("You do not have permission to access this resource");
            }
            logger.info("User {} granted access to {} {}", userEmail, httpMethod, path);
            return true;
        } catch (IllegalArgumentException e) {
            logger.error("Access denied: {}", e.getMessage());
            exceptionResolver.resolveException(request, response, handler, e);
            return false;
        }
    }
}





