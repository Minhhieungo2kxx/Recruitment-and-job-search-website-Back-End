package com.webjob.application.service;

import com.webjob.application.dto.Request.ForgotPasswordRequest;
import com.webjob.application.dto.Request.OutboxDTO;
import com.webjob.application.dto.Request.ResetPasswordRequest;
import com.webjob.application.exception.Customs.BadRequestException;
import com.webjob.application.exception.Customs.ResourceNotFoundException;
import com.webjob.application.messaging.producer.EmailProducer;
import com.webjob.application.models.Entity.User;
import com.webjob.application.repository.UserRepository;
import com.webjob.application.service.OutBox.OutboxService;
import com.webjob.application.service.Redis.PasswordResetRedisService;
import com.webjob.application.service.SendEmail.EmailService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Duration;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
@ExtendWith(MockitoExtension.class)
@DisplayName("PasswordResetService Unit Tests")
public class PasswordResetServiceTest {
    private static final Long USER_ID = 1L;
    private static final String TOKEN = "valid-reset-token";
    private static final String OLD_PASSWORD = "OldPassword@123";
    private static final String NEW_PASSWORD = "NewPassword@123";
    private static final String ENCODED_PASSWORD = "encoded-new-password";
    private static final String EMAIL = "user@example.com";

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private EmailService emailService;

    @Mock
    private EmailProducer emailProducer;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private PasswordResetRedisService redisService;

    @Mock
    private OutboxService outboxService;

    @InjectMocks
    private PasswordResetService passwordResetService;

    private User user;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(USER_ID);
        user.setEmail(EMAIL);
        user.setFullName("Test User");
        user.setPassword("encoded-old-password");
        user.setRefreshToken("old-refresh-token");
    }

    // ==================== Test helpers ====================

    private ResetPasswordRequest resetRequest() {
        ResetPasswordRequest request = new ResetPasswordRequest();
        request.setToken(TOKEN);
        request.setOldPassword(OLD_PASSWORD);
        request.setNewPassword(NEW_PASSWORD);
        request.setConfirmNewPassword(NEW_PASSWORD);
        return request;
    }

    private void givenValidTokenAndUser() {
        when(redisService.getUserId(TOKEN)).thenReturn(USER_ID);
        when(userRepository.findById(USER_ID))
                .thenReturn(java.util.Optional.of(user));
    }

    // ==================== resetPassword ====================

    @Test
    void resetPassword_shouldThrowException_whenTokenIsInvalid() {
        ResetPasswordRequest request = resetRequest();

        when(redisService.getUserId(TOKEN)).thenReturn(null);

        assertThrows(
                BadRequestException.class,
                () -> passwordResetService.resetPassword(request)
        );

        verify(userRepository, never()).findById(anyLong());
        verify(userRepository, never()).save(any());
        verify(redisService, never()).delete(anyLong(), anyString());
    }

    @Test
    void resetPassword_shouldThrowException_whenUserDoesNotExist() {
        ResetPasswordRequest request = resetRequest();

        when(redisService.getUserId(TOKEN)).thenReturn(USER_ID);
        when(userRepository.findById(USER_ID)).thenReturn(
                java.util.Optional.empty()
        );

        assertThrows(
                ResourceNotFoundException.class,
                () -> passwordResetService.resetPassword(request)
        );

        verify(passwordEncoder, never()).matches(anyString(), anyString());
        verify(userRepository, never()).save(any());
    }

    @Test
    void resetPassword_shouldThrowException_whenOldPasswordIsIncorrect() {
        ResetPasswordRequest request = resetRequest();

        givenValidTokenAndUser();
        when(passwordEncoder.matches(OLD_PASSWORD, user.getPassword()))
                .thenReturn(false);

        BadRequestException exception = assertThrows(
                BadRequestException.class,
                () -> passwordResetService.resetPassword(request)
        );

        assertEquals("Mật khẩu cũ không đúng", exception.getMessage());

        verify(passwordEncoder, never()).encode(anyString());
        verify(userRepository, never()).save(any());
        verify(redisService, never()).delete(anyLong(), anyString());
    }

    @Test
    void resetPassword_shouldThrowException_whenNewPasswordConfirmationDoesNotMatch() {
        ResetPasswordRequest request = resetRequest();
        request.setConfirmNewPassword("DifferentPassword@123");

        givenValidTokenAndUser();
        when(passwordEncoder.matches(OLD_PASSWORD, user.getPassword()))
                .thenReturn(true);

        BadRequestException exception = assertThrows(
                BadRequestException.class,
                () -> passwordResetService.resetPassword(request)
        );

        assertEquals("Xác nhận mật khẩu không khớp", exception.getMessage());

        verify(passwordEncoder, never()).encode(anyString());
        verify(userRepository, never()).save(any());
        verify(redisService, never()).delete(anyLong(), anyString());
    }

    @Test
    void resetPassword_shouldThrowException_whenNewPasswordEqualsOldPassword() {
        ResetPasswordRequest request = resetRequest();
        request.setNewPassword(OLD_PASSWORD);
        request.setConfirmNewPassword(OLD_PASSWORD);

        givenValidTokenAndUser();
        when(passwordEncoder.matches(OLD_PASSWORD, user.getPassword()))
                .thenReturn(true);

        BadRequestException exception = assertThrows(
                BadRequestException.class,
                () -> passwordResetService.resetPassword(request)
        );

        assertEquals(
                "Mật khẩu mới không được trùng với mật khẩu cũ",
                exception.getMessage()
        );

        verify(passwordEncoder, never()).encode(anyString());
        verify(userRepository, never()).save(any());
        verify(redisService, never()).delete(anyLong(), anyString());
    }

    @Test
    void resetPassword_shouldUpdatePasswordAndInvalidateTokens_whenRequestIsValid() {
        ResetPasswordRequest request = resetRequest();

        givenValidTokenAndUser();
        when(passwordEncoder.matches(OLD_PASSWORD, user.getPassword()))
                .thenReturn(true);
        when(passwordEncoder.encode(NEW_PASSWORD)).thenReturn(ENCODED_PASSWORD);

        passwordResetService.resetPassword(request);

        assertEquals(ENCODED_PASSWORD, user.getPassword());
        assertNull(user.getRefreshToken());

        verify(passwordEncoder).encode(NEW_PASSWORD);
        verify(userRepository).save(user);
        verify(redisService).delete(USER_ID, TOKEN);
    }



    @Test
    void forgotPassword_shouldThrowException_whenUserDoesNotExist() {
        ForgotPasswordRequest request = new ForgotPasswordRequest();
        request.setEmail(EMAIL);

        when(userRepository.findByEmail(EMAIL)).thenReturn(null);

        assertThrows(
                ResourceNotFoundException.class,
                () -> passwordResetService.forgotPassword(request)
        );

        verify(redisService, never()).save(
                anyLong(),
                anyString(),
                any(Duration.class)
        );
        verify(outboxService, never()).save(any());
    }
    @Test
    void resetPassword_shouldNotSaveUser_whenPasswordEncodingFails() {
        ResetPasswordRequest request = resetRequest();

        givenValidTokenAndUser();

        when(passwordEncoder.matches(OLD_PASSWORD, user.getPassword()))
                .thenReturn(true);

        when(passwordEncoder.encode(NEW_PASSWORD))
                .thenThrow(new IllegalStateException("Encoding failed"));

        assertThrows(
                IllegalStateException.class,
                () -> passwordResetService.resetPassword(request)
        );

        verify(userRepository, never()).save(any());
        verify(redisService, never()).delete(anyLong(), anyString());
    }

    @Test
    void resetPassword_shouldPropagateException_whenRedisLookupFails() {
        ResetPasswordRequest request = resetRequest();

        when(redisService.getUserId(TOKEN))
                .thenThrow(new IllegalStateException("Redis unavailable"));

        assertThrows(
                IllegalStateException.class,
                () -> passwordResetService.resetPassword(request)
        );

        verifyNoInteractions(passwordEncoder);
        verify(userRepository, never()).save(any());
    }
    @Test
    void forgotPassword_shouldPropagateException_whenOutboxSaveFails() {
        ForgotPasswordRequest request = new ForgotPasswordRequest();
        request.setEmail(EMAIL);

        when(userRepository.findByEmail(EMAIL)).thenReturn(user);

        doThrow(new IllegalStateException("Outbox unavailable"))
                .when(outboxService)
                .save(any(OutboxDTO.class));

        assertThrows(
                IllegalStateException.class,
                () -> passwordResetService.forgotPassword(request)
        );

        verify(redisService).save(
                eq(USER_ID),
                anyString(),
                eq(Duration.ofMinutes(6))
        );
        verify(outboxService).save(any(OutboxDTO.class));
    }
    @Test
    void resetPassword_shouldPropagateException_whenUserSaveFails() {
        ResetPasswordRequest request = resetRequest();

        givenValidTokenAndUser();

        when(passwordEncoder.matches(OLD_PASSWORD, user.getPassword()))
                .thenReturn(true);

        when(passwordEncoder.encode(NEW_PASSWORD))
                .thenReturn(ENCODED_PASSWORD);

        when(userRepository.save(user))
                .thenThrow(new IllegalStateException("Database unavailable"));

        assertThrows(
                IllegalStateException.class,
                () -> passwordResetService.resetPassword(request)
        );

        verify(userRepository).save(user);
    }

    @Test
    void forgotPassword_shouldGenerateUniqueToken_whenCalledMultipleTimes() {
        ForgotPasswordRequest request = new ForgotPasswordRequest();
        request.setEmail(EMAIL);

        when(userRepository.findByEmail(EMAIL)).thenReturn(user);

        passwordResetService.forgotPassword(request);
        passwordResetService.forgotPassword(request);

        ArgumentCaptor<String> tokenCaptor = ArgumentCaptor.forClass(String.class);

        verify(redisService, times(2)).save(
                eq(USER_ID),
                tokenCaptor.capture(),
                eq(Duration.ofMinutes(6))
        );

        assertEquals(2, tokenCaptor.getAllValues().size());
        assertNotEquals(
                tokenCaptor.getAllValues().get(0),
                tokenCaptor.getAllValues().get(1),
                "Mỗi yêu cầu quên mật khẩu cần tạo token mới"
        );
    }
}
