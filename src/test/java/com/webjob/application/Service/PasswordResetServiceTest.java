package com.webjob.application.Service;

import com.webjob.application.Dto.Request.ResetPasswordRequest;
import com.webjob.application.Model.Entity.PasswordResetToken;
import com.webjob.application.Model.Entity.User;
import com.webjob.application.Repository.PasswordResetTokenRepository;
import com.webjob.application.Repository.UserRepository;
import com.webjob.application.Service.SendEmail.EmailService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class PasswordResetServiceTest {
    @Mock private UserService userService;
    @Mock private PasswordResetTokenRepository tokenRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private EmailService emailService;
    @Mock private UserRepository userRepository;

    @InjectMocks
    private PasswordResetService passwordResetService;

    private User user;
    private PasswordResetToken oldToken;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1L);
        user.setFullName("John Doe");
        user.setEmail("john.doe@example.com");

        oldToken = new PasswordResetToken();
        oldToken.setId(1L);
        oldToken.setToken("old-token");
        oldToken.setUser(user);
        oldToken.setUsed(false);
        oldToken.setExpiresAt(Instant.now().plusSeconds(600));
    }

    @Nested
    class ForgotPasswordTests {

        @Test
        void shouldDeleteOldTokenAndSendEmail_WhenOldTokenExists() {
            // Giả lập behavior
            when(tokenRepository.findByUser(user)).thenReturn(Optional.of(oldToken));

            ArgumentCaptor<PasswordResetToken> tokenCaptor = ArgumentCaptor.forClass(PasswordResetToken.class);
            ArgumentCaptor<Map<String, Object>> emailCaptor = ArgumentCaptor.forClass(Map.class);

            // Gọi hàm cần test
            passwordResetService.forgotPassword(user);

            // // Kiểm tra hành vi
            verify(tokenRepository).delete(oldToken);
            verify(tokenRepository).save(tokenCaptor.capture());
            PasswordResetToken savedToken = tokenCaptor.getValue();

//            Kiểm tra dữ liệu
            assertNotNull(savedToken.getToken());
            assertEquals(user, savedToken.getUser());
            assertFalse(savedToken.isUsed());
            assertTrue(savedToken.getExpiresAt().isAfter(Instant.now()));

            verify(emailService).sendTemplateResetPassword(
                    eq("Password Reset Request"),
                    eq("emails/password-reset"),
                    emailCaptor.capture()
            );
            Map<String, Object> emailVars = emailCaptor.getValue();
            assertEquals(user.getFullName(), emailVars.get("name"));
            assertEquals(user.getEmail(), emailVars.get("username"));
            assertEquals(savedToken.getToken(), emailVars.get("token"));
            assertTrue(((String) emailVars.get("resetLink")).contains(savedToken.getToken()));
        }

        @Test
        void shouldCreateTokenAndSendEmail_WhenNoOldTokenExists() {
            when(tokenRepository.findByUser(user)).thenReturn(Optional.empty());

            ArgumentCaptor<PasswordResetToken> tokenCaptor = ArgumentCaptor.forClass(PasswordResetToken.class);
            ArgumentCaptor<Map<String, Object>> emailCaptor = ArgumentCaptor.forClass(Map.class);

            passwordResetService.forgotPassword(user);

            verify(tokenRepository, never()).delete(any());
            verify(tokenRepository).save(tokenCaptor.capture());
            verify(emailService).sendTemplateResetPassword(
                    eq("Password Reset Request"),
                    eq("emails/password-reset"),
                    emailCaptor.capture()
            );

            PasswordResetToken savedToken = tokenCaptor.getValue();
            assertNotNull(savedToken.getToken());
            assertEquals(user, savedToken.getUser());
        }
    }

    @Nested
    class ResetPasswordTests {

        @Test
        void shouldEncodePasswordAndMarkTokenUsed_WhenValidToken() {
            PasswordResetToken token = new PasswordResetToken();
            token.setToken("valid-token");
            token.setUser(user);
            token.setUsed(false);
            token.setExpiresAt(Instant.now().plusSeconds(300));

            ResetPasswordRequest request = new ResetPasswordRequest();
            request.setToken(token.getToken());
            request.setNewPassword("newPassword123");

            when(tokenRepository.findByToken(token.getToken())).thenReturn(Optional.of(token));
            when(passwordEncoder.encode("newPassword123")).thenReturn("encodedPass");

            ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
            ArgumentCaptor<PasswordResetToken> tokenCaptor = ArgumentCaptor.forClass(PasswordResetToken.class);

            passwordResetService.resetPassword(request);

            verify(userRepository).save(userCaptor.capture());
            User savedUser = userCaptor.getValue();
            assertEquals("encodedPass", savedUser.getPassword());

            verify(tokenRepository).save(tokenCaptor.capture());
            PasswordResetToken savedToken = tokenCaptor.getValue();
            assertTrue(savedToken.isUsed());
        }

        @Test
        void shouldThrow_WhenTokenExpiredOrUsed() {
            PasswordResetToken expiredToken = new PasswordResetToken();
            expiredToken.setToken("expired-token");
            expiredToken.setUser(user);
            expiredToken.setUsed(true);
            expiredToken.setExpiresAt(Instant.now().minusSeconds(10));

            when(tokenRepository.findByToken(expiredToken.getToken()))
                    .thenReturn(Optional.of(expiredToken));

            ResetPasswordRequest request = new ResetPasswordRequest();
            request.setToken(expiredToken.getToken());
            request.setNewPassword("pass123");

            RuntimeException ex = assertThrows(RuntimeException.class,
                    () -> passwordResetService.resetPassword(request));
            assertEquals("Token expired or already used", ex.getMessage());
        }

        @Test
        void shouldThrow_WhenTokenNotFound() {
            when(tokenRepository.findByToken("invalid-token")).thenReturn(Optional.empty());

            ResetPasswordRequest request = new ResetPasswordRequest();
            request.setToken("invalid-token");
            request.setNewPassword("pass123");

            RuntimeException ex = assertThrows(RuntimeException.class,
                    () -> passwordResetService.resetPassword(request));
            assertEquals("Invalid token", ex.getMessage());
        }
    }

}
//[Test Method]
//        │
//        │  when(tokenRepository.findByToken("invalid-token")) → Optional.empty()
//    │
//            ├──► passwordResetService.resetPassword(request)
//    │        │
//            │        ├──► tokenRepository.findByToken("invalid-token")
//    │        │        └── returns Optional.empty()
//    │        │
//            │        └──► .orElseThrow(...) → throws RuntimeException("Invalid token")
//    │
//            └──► assertThrows(...) catches exception
//              │
//                      └──► assertEquals("Invalid token", ex.getMessage())


//sử dụng JUnit 5 kết hợp với Mockito để kiểm thử logic của lớp PasswordResetService.