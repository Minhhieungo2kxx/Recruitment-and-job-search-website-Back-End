package com.webjob.application.Services;


import com.webjob.application.Models.Response.LoginResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;


@Service
public class SecurityUtil {

    @Value("${security.jwt.base64-secret}")
    private String jwtKey;

    @Value("${security.jwt.access-token-validity-in-seconds}")
    private Long jwtaccessExpiration;

    @Value("${security.jwt.refresh-token-validity-in-seconds}")
    private Long jwtrefreshExpiration;
    public static final MacAlgorithm JWT_ALGORITHM = MacAlgorithm.HS512;

    private final JwtEncoder jwtEncoder;

    public SecurityUtil(JwtEncoder jwtEncoder) {
        this.jwtEncoder = jwtEncoder;
    }


    public String createacessToken(String email,LoginResponse.User user) {
        Instant now = Instant.now();
        Instant validity = now.plus(jwtaccessExpiration, ChronoUnit.SECONDS);
        LoginResponse.UserinsideToken userinsideToken=new LoginResponse.UserinsideToken();
        userinsideToken.setId(user.getId());
        userinsideToken.setEmail(user.getEmail());
        userinsideToken.setUsername(user.getFullName());
// @formatter:off
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuedAt(now)
                .expiresAt(validity)
                .subject(email)
                .claim("user",userinsideToken)
//                .claim("admin", authentication.getAuthorities().stream()
//                        .map(GrantedAuthority::getAuthority)
//                        .collect(toList()))
               .build();
        JwsHeader jwsHeader = JwsHeader.with(JWT_ALGORITHM).build();
        return this.jwtEncoder.encode(JwtEncoderParameters.from(jwsHeader,
                claims)).getTokenValue();
    }
    public String createrefreshToken(String email,LoginResponse.User user) {
        Instant now = Instant.now();
        Instant validity = now.plus(jwtrefreshExpiration, ChronoUnit.SECONDS);
        LoginResponse.UserinsideToken userinsideToken=new LoginResponse.UserinsideToken();
        userinsideToken.setId(user.getId());
        userinsideToken.setEmail(user.getEmail());
        userinsideToken.setUsername(user.getFullName());
// @formatter:off
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuedAt(now)
                .expiresAt(validity)
                .subject(email)
                .claim("user",userinsideToken)
//                .claim("admin", authentication.getAuthorities().stream()
//                        .map(GrantedAuthority::getAuthority)
//                        .collect(toList()))
                .build();
        JwsHeader jwsHeader = JwsHeader.with(JWT_ALGORITHM).build();
        return this.jwtEncoder.encode(JwtEncoderParameters.from(jwsHeader,
                claims)).getTokenValue();
    }


}
