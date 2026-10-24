package com.webjob.application.service;


import com.webjob.application.component.SecurityUtils;
import com.webjob.application.dto.Request.LoginDTO;
import com.webjob.application.dto.Response.LoginResponse;
import com.webjob.application.enums.UserStatus;
import com.webjob.application.exception.Customs.UnauthorizedException;
import com.webjob.application.models.Entity.User;
import com.webjob.application.service.Redis.LoginNotificationService;
import com.webjob.application.service.Redis.TokenBlacklistService;
import com.webjob.application.service.SendEmail.ApplicationEmailService;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.Test;

import org.mockito.InjectMocks;
import org.mockito.Mock;

import org.modelmapper.ModelMapper;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService Unit Tests")
public class AuthServiceTest {
    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private UserService userService;

    @Mock
    private ModelMapper modelMapper;

    @Mock
    private SecurityUtils securityUtils;

    @Mock
    private JwtDecoder jwtDecoder;

    @Mock
    private TokenBlacklistService tokenBlacklistService;

    @Mock
    private ApplicationEmailService applicationEmailService;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private LoginNotificationService loginNotificationService;

    @InjectMocks
    private AuthService authService;

    @Test
    void extractBearerToken_shouldReturnToken_whenHeaderIsValid() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getHeader("Authorization"))
                .thenReturn("Bearer access-token");

        String result = authService.extractBearerToken(request);

        assertThat(result).isEqualTo("access-token");
    }
    @Test
    void extractBearerToken_shouldReturnNull_whenHeaderIsMissing() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getHeader("Authorization")).thenReturn(null);

        assertThat(authService.extractBearerToken(request)).isNull();
    }
    @Test
    void extractBearerToken_shouldReturnNull_whenHeaderDoesNotUseBearerScheme() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getHeader("Authorization"))
                .thenReturn("Basic abc123");

        assertThat(authService.extractBearerToken(request)).isNull();
    }

    @Test
    void getClientIp_shouldReturnFirstIp_whenForwardedHeaderHasMultipleIps() {
        HttpServletRequest request = mock(HttpServletRequest.class);

        when(request.getHeader("X-Forwarded-For"))
                .thenReturn("203.0.113.10, 10.0.0.2");

        assertThat(authService.getClientIp(request))
                .isEqualTo("203.0.113.10");
    }

    @Test
    void getClientIp_shouldFallbackToRemoteAddress_whenNoForwardedHeaderExists() {
        HttpServletRequest request = mock(HttpServletRequest.class);

        when(request.getRemoteAddr()).thenReturn("192.168.1.10");

        assertThat(authService.getClientIp(request))
                .isEqualTo("192.168.1.10");
    }

    @Test
    void clearRefreshCookie_shouldReturnExpiredRefreshCookie() {
        HttpHeaders headers = authService.clearRefreshCookie();

        List<String> cookies = headers.get(HttpHeaders.SET_COOKIE);

        assertThat(cookies).hasSize(1);
        assertThat(cookies.get(0))
                .contains("refresh=")
                .contains("Max-Age=0")
                .contains("HttpOnly")
                .contains("Path=/")
                .contains("Domain=localhost")
                .contains("Secure");
    }
    @Test
    void getUserFromAuthentication_shouldReturnUser_whenStatusIsActive() {
        Authentication authentication = mock(Authentication.class);
        User user = mock(User.class);

        when(authentication.getName()).thenReturn("1");
        when(userService.getById(1L)).thenReturn(user);
        when(user.getStatus()).thenReturn(UserStatus.ACTIVE);

        User result = authService.getUserFromAuthentication(authentication);

        assertThat(result).isSameAs(user);
    }

    @ParameterizedTest
    @EnumSource(
            value = UserStatus.class,
            names = {"BLOCKED", "PENDING", "INACTIVE"}
    )
    void getUserFromAuthentication_shouldThrowUnauthorized_whenAccountIsNotActive(
            UserStatus status
    ) {
        Authentication authentication = mock(Authentication.class);
        User user = mock(User.class);

        when(authentication.getName()).thenReturn("1");
        when(userService.getById(1L)).thenReturn(user);
        when(user.getStatus()).thenReturn(status);

        assertThatThrownBy(
                () -> authService.getUserFromAuthentication(authentication)
        ).isInstanceOf(UnauthorizedException.class);
    }


    @Test
    void refreshToken_shouldThrowUnauthorized_whenTokenIsNull() {
        assertThatThrownBy(() -> authService.refreshToken(null))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessage("No refresh token found");

        verifyNoInteractions(userService, jwtDecoder);
    }
    @ParameterizedTest
    @ValueSource(strings = {"", " ", "default"})
    void refreshToken_shouldThrowUnauthorized_whenTokenIsBlankOrDefault(
            String refreshToken
    ) {
        assertThatThrownBy(() -> authService.refreshToken(refreshToken))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessage("No refresh token found");

        verifyNoInteractions(userService, jwtDecoder);
    }

    @Test
    void refreshToken_shouldThrowUnauthorized_whenTokenHashDoesNotMatchAnyUser() {
        String refreshToken = "refresh-token";
        String tokenHash = "hashed-refresh-token";

        when(securityUtils.sha256(refreshToken)).thenReturn(tokenHash);
        when(userService.getUserByRefreshTokenHash(tokenHash))
                .thenThrow(new UsernameNotFoundException("User not found"));

        assertThatThrownBy(() -> authService.refreshToken(refreshToken))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessage("Refresh token expired or revoked");

        verifyNoInteractions(jwtDecoder);
    }
    @Test
    void refreshToken_shouldThrowUnauthorized_whenJwtIsInvalid() {
        String refreshToken = "invalid-token";
        User user = mock(User.class);

        when(securityUtils.sha256(refreshToken)).thenReturn("token-hash");
        when(userService.getUserByRefreshTokenHash("token-hash"))
                .thenReturn(user);
        when(jwtDecoder.decode(refreshToken))
                .thenThrow(new JwtException("Invalid JWT"));

        assertThatThrownBy(() -> authService.refreshToken(refreshToken))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessage("Invalid refresh token.");
    }

    @Test
    void refreshToken_shouldThrowUnauthorized_whenEmailDoesNotMatch() {
        String refreshToken = "refresh-token";
        User user = mock(User.class);
        Jwt jwt = mock(Jwt.class);

        when(user.getEmail()).thenReturn("correct@example.com");
        when(securityUtils.sha256(refreshToken)).thenReturn("token-hash");
        when(userService.getUserByRefreshTokenHash("token-hash"))
                .thenReturn(user);
        when(jwtDecoder.decode(refreshToken)).thenReturn(jwt);
        when(jwt.getClaim("email")).thenReturn("other@example.com");

        assertThatThrownBy(() -> authService.refreshToken(refreshToken))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessage("Refresh token does not match user");

        verify(userService, never()).updateRefreshtoken(anyLong(), any());
    }

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(
                authService,
                "jwtRefreshExpiration",
                3600L
        );
    }
    @Test
    void refreshToken_shouldRotateRefreshTokenAndReturnNewAccessToken() {
        String oldRefreshToken = "old-refresh-token";
        String oldHash = "old-hash";

        String newRefreshToken = "new-refresh-token";
        String newHash = "new-hash";

        User user = mock(User.class);
        Jwt jwt = mock(Jwt.class);
        LoginResponse.User userDTO = mock(LoginResponse.User.class);

        when(user.getId()).thenReturn(1L);
        when(user.getEmail()).thenReturn("user@example.com");

        when(securityUtils.sha256(oldRefreshToken)).thenReturn(oldHash);

        when(userService.getUserByRefreshTokenHash(oldHash)).thenReturn(user);
        when(jwtDecoder.decode(oldRefreshToken)).thenReturn(jwt);
        when(jwt.getClaim("email")).thenReturn("user@example.com");

        when(modelMapper.map(user, LoginResponse.User.class)).thenReturn(userDTO);
        when(securityUtils.createacessToken(user)).thenReturn("new-access-token");
        when(securityUtils.createrefreshToken(user)).thenReturn(newRefreshToken);
        when(securityUtils.sha256(newRefreshToken)).thenReturn(newHash);

        LoginResponse response = authService.refreshToken(oldRefreshToken);

        assertThat(response.getAccessToken()).isEqualTo("new-access-token");
        assertThat(response.getRefreshCookie())
                .contains("refresh=" + newRefreshToken)
                .contains("HttpOnly")
                .contains("Max-Age=3600");

        verify(userService).updateRefreshtoken(1L, newHash);
    }

    @Test
    void logout_shouldThrowUnauthorized_whenUserIsNotAuthenticated() {
        SecurityContext securityContext = mock(SecurityContext.class);

        try (MockedStatic<SecurityContextHolder> mockedContext =
                     mockStatic(SecurityContextHolder.class)) {

            mockedContext.when(SecurityContextHolder::getContext)
                    .thenReturn(securityContext);

            when(securityContext.getAuthentication()).thenReturn(null);

            HttpServletRequest request = mock(HttpServletRequest.class);

            assertThatThrownBy(() -> authService.logout(request))
                    .isInstanceOf(UnauthorizedException.class);

            verifyNoInteractions(tokenBlacklistService, userService);
        }
    }

    @Test
    void logout_shouldBlacklistAccessTokenAndClearRefreshToken() {
        SecurityContext securityContext = mock(SecurityContext.class);
        Authentication authentication = mock(Authentication.class);
        User user = mock(User.class);
        HttpServletRequest request = mock(HttpServletRequest.class);

        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getName()).thenReturn("1");
        when(request.getHeader("Authorization")).thenReturn("Bearer access-token");
        when(userService.getById(1L)).thenReturn(user);
        when(user.getStatus()).thenReturn(UserStatus.ACTIVE);
        when(user.getId()).thenReturn(1L);
        when(securityUtils.getRemainingValidity("access-token")).thenReturn(300L);

        try (MockedStatic<SecurityContextHolder> mockedContext =
                     mockStatic(SecurityContextHolder.class)) {

            mockedContext.when(SecurityContextHolder::getContext)
                    .thenReturn(securityContext);

            authService.logout(request);
        }

        verify(tokenBlacklistService).blacklistToken("access-token", 300L);
        verify(userService).updateRefreshtoken(1L, null);
    }

    @Test
    void logout_shouldClearRefreshTokenWithoutBlacklisting_whenBearerTokenIsMissing() {
        SecurityContext securityContext = mock(SecurityContext.class);
        Authentication authentication = mock(Authentication.class);
        User user = mock(User.class);
        HttpServletRequest request = mock(HttpServletRequest.class);

        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getName()).thenReturn("1");
        when(request.getHeader("Authorization")).thenReturn(null);
        when(userService.getById(1L)).thenReturn(user);
        when(user.getStatus()).thenReturn(UserStatus.ACTIVE);
        when(user.getId()).thenReturn(1L);

        try (MockedStatic<SecurityContextHolder> mockedContext =
                     mockStatic(SecurityContextHolder.class)) {

            mockedContext.when(SecurityContextHolder::getContext)
                    .thenReturn(securityContext);

            authService.logout(request);
        }

        verify(userService).updateRefreshtoken(1L, null);
        verifyNoInteractions(tokenBlacklistService);
    }

    @Test
    void handleLogin_shouldPropagateAuthenticationException_whenCredentialsAreInvalid() {
        LoginDTO loginDTO = mock(LoginDTO.class);
        HttpServletRequest request = mock(HttpServletRequest.class);

        when(loginDTO.getUsername()).thenReturn("user");
        when(loginDTO.getPassword()).thenReturn("wrong-password");

        when(authenticationManager.authenticate(any(
                UsernamePasswordAuthenticationToken.class
        ))).thenThrow(new BadCredentialsException("Bad credentials"));

        assertThatThrownBy(() -> authService.handleLogin(loginDTO, request))
                .isInstanceOf(BadCredentialsException.class);

        verifyNoInteractions(userService, eventPublisher);
    }



}
