package com.webjob.application.Config;

import com.webjob.application.Dto.Request.Redis.PermissionSet;
import com.webjob.application.Service.Redis.PermissionCacheService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;

import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

@Component
@RequiredArgsConstructor
public class PermissionAuthorizationManager implements AuthorizationManager<RequestAuthorizationContext> {
    private final PermissionCacheService permissionCacheService;
    private final AntPathMatcher matcher = new AntPathMatcher();

    @Override
    public AuthorizationDecision check(
            Supplier<Authentication> authenticationSupplier,
            RequestAuthorizationContext context) {

        Authentication authentication = authenticationSupplier.get();

        if (!(authentication instanceof JwtAuthenticationToken jwtAuth)) {
            return new AuthorizationDecision(false);
        }

        Jwt jwt = jwtAuth.getToken();

        String userId = jwt.getSubject();

        if (userId == null) {
            return new AuthorizationDecision(false);
        }

        PermissionSet permissionSet =
                permissionCacheService.getPermissions(userId);

        HttpServletRequest request = context.getRequest();

        boolean granted = permissionSet.match(
                request.getMethod(),
                request.getRequestURI()
        );

        return new AuthorizationDecision(granted);
    }


}
