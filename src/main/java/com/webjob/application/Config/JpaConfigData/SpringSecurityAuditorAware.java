package com.webjob.application.Config.JpaConfigData;

import com.webjob.application.Config.CustomUserDetails;
import org.springframework.data.domain.AuditorAware;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.util.Optional;

public class SpringSecurityAuditorAware implements AuditorAware<String> {

    @Override
    public Optional<String> getCurrentAuditor() {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth == null || !auth.isAuthenticated()
                || auth.getPrincipal().equals("anonymousUser")) {
            return Optional.empty();
        }

        // JWT (Resource Server)
        if (auth instanceof JwtAuthenticationToken jwtAuth) {
            Jwt jwt = jwtAuth.getToken();
            // nếu trong token bạn có claim email
            String email = jwt.getClaimAsString("email");
            return Optional.ofNullable(email);
        }

        // OAuth2 Login (Google)
        if (auth.getPrincipal() instanceof OAuth2User oauth2User) {
            return Optional.ofNullable(oauth2User.getAttribute("email"));
        }

        // CustomUserDetails (form login nếu có)
        if (auth.getPrincipal() instanceof CustomUserDetails customUser) {
            return Optional.ofNullable(customUser.getEmail());
        }

        return Optional.empty();
    }
}
