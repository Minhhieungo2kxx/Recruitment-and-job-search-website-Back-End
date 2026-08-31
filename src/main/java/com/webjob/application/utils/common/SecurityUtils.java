package com.webjob.application.utils.common;

import com.webjob.application.enums.CompanyStatus;
import com.webjob.application.exception.Customs.ForbiddenException;
import com.webjob.application.exception.Customs.ResourceNotFoundException;
import com.webjob.application.exception.Customs.UnauthorizedException;
import com.webjob.application.models.Entity.Company;
import com.webjob.application.models.Entity.User;
import com.webjob.application.repository.UserRepository;
import com.webjob.application.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Component
@RequiredArgsConstructor
public class SecurityUtils {
    private final UserRepository userRepository;
    @Value("${security.jwt.base64-secret}")
    private String jwtKey;

    @Value("${security.jwt.access-token-validity-in-seconds}")
    private Long jwtaccessExpiration;

    @Value("${security.jwt.refresh-token-validity-in-seconds}")
    private Long jwtrefreshExpiration;
    public static final MacAlgorithm JWT_ALGORITHM = MacAlgorithm.HS512;

    private final JwtEncoder jwtEncoder;

    private final JwtDecoder jwtDecoder;

    public User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null
                || !authentication.isAuthenticated()) {
            return null;
        }
        String username = authentication.getName();
        if ("anonymousUser".equals(username)) {
            return null;
        }
        Long userId = Long.valueOf(authentication.getName());
        User user=userRepository.findByIdAndDeletedFalse(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found or no Active  with id: " + userId));

        switch (user.getStatus()) {
            case ACTIVE:
                return user;

            case BLOCKED:
                throw new UnauthorizedException("Your account has been blocked.");

            case PENDING:
                throw new UnauthorizedException("Your account is pending verification.");

            case INACTIVE:
                throw new UnauthorizedException("Your account has been deactivated.");

            default:
                throw new UnauthorizedException("Invalid account status.");
        }

    }
    public User getUserId(Long userId) {
        User user=userRepository.findByIdAndDeletedFalse(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found or no Active  with id: " + userId));

        switch (user.getStatus()) {
            case ACTIVE:
                return user;

            case BLOCKED:
                throw new UnauthorizedException("Your account has been blocked.");

            case PENDING:
                throw new UnauthorizedException("Your account is pending verification.");

            case INACTIVE:
                throw new UnauthorizedException("Your account has been deactivated.");

            default:
                throw new UnauthorizedException("Invalid account status.");
        }

    }

    public Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext()
                .getAuthentication();

        if (authentication == null ||!authentication.isAuthenticated()) {
            return null;
        }

        String username = authentication.getName();

        if ("anonymousUser".equals(username)) {
            return null;
        }
        try {
            return Long.parseLong(username);
        } catch (NumberFormatException e) {
            return null;
        }
    }
    public  boolean isAuthenticated() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        return authentication != null
                && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken);
    }

    public String createacessToken(User user) {
        Instant now = Instant.now();
        Instant validity = now.plus(jwtaccessExpiration, ChronoUnit.SECONDS);
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuedAt(now)
                .expiresAt(validity)
                .subject(user.getId().toString())
                .claim("email",user.getEmail().trim())
                .claim("username",user.getFullName().trim())
                .claim("roles", List.of(user.getRole().getCode().trim().toUpperCase()))
                .build();
        JwsHeader jwsHeader = JwsHeader.with(JWT_ALGORITHM).build();
        return this.jwtEncoder.encode(JwtEncoderParameters.from(jwsHeader,
                claims)).getTokenValue();
    }
    public String createrefreshToken(User user) {
        Instant now = Instant.now();
        Instant validity = now.plus(jwtrefreshExpiration, ChronoUnit.SECONDS);

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuedAt(now)
                .expiresAt(validity)
                .subject(user.getId().toString())
                .claim("email",user.getEmail().trim())
                .claim("username",user.getFullName().trim())
                .claim("roles", List.of(user.getRole().getCode().trim().toUpperCase()))
                .build();
        JwsHeader jwsHeader = JwsHeader.with(JWT_ALGORITHM).build();
        return this.jwtEncoder.encode(JwtEncoderParameters.from(jwsHeader,
                claims)).getTokenValue();
    }
    public long getRemainingValidity(String token) {
        Jwt jwt = jwtDecoder.decode(token);
        Instant expiration = jwt.getExpiresAt();
        if (expiration == null) return 0;
        return Duration.between(Instant.now(), expiration).getSeconds();
    }
    public  String sha256(String value) {

        try {

            MessageDigest digest = MessageDigest.getInstance("SHA-256");

            byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));

            StringBuilder hexString = new StringBuilder(64);

            for (byte b : hash) {

                String hex = Integer.toHexString(0xff & b);

                if (hex.length() == 1) {
                    hexString.append('0');
                }

                hexString.append(hex);
            }

            return hexString.toString();

        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm not available", e);
        }
    }

}
