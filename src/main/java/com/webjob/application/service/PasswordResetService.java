package com.webjob.application.service;

import com.webjob.application.dto.Request.ForgotPasswordRequest;
import com.webjob.application.dto.Response.ApiResponse;
import com.webjob.application.messaging.dto.ForgotPasswordEmailEvent;
import com.webjob.application.messaging.producer.EmailProducer;
import com.webjob.application.models.Entity.PasswordResetToken;
import com.webjob.application.models.Entity.User;
import com.webjob.application.dto.Request.ResetPasswordRequest;
import com.webjob.application.repository.PasswordResetTokenRepository;
import com.webjob.application.repository.UserRepository;
import com.webjob.application.service.SendEmail.EmailService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestBody;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class PasswordResetService {

    private final UserService userService;

    private final PasswordResetTokenRepository tokenRepository;

    private final PasswordEncoder passwordEncoder;

    private final EmailService emailService;

    private final UserRepository userRepository;
    private final EmailProducer emailProducer;
    private final ApplicationEventPublisher eventPublisher;


    public void forgotPassword(User user) {
        // Generate token
        //  Xoá token cũ nếu tồn tại
        tokenRepository.findByUser(user).ifPresent(tokenRepository::delete);

//        dat lai PasswordResetToken new
        String token = generate8CharToken();
        Instant expiresAt = Instant.now().plusSeconds(60 * 5); // expires in 5 mints
        PasswordResetToken resetToken = new PasswordResetToken();
        resetToken.setToken(token);
        resetToken.setExpiresAt(expiresAt);
        resetToken.setUser(user);
        resetToken.setUsed(false);
        tokenRepository.save(resetToken);
//        send Email
        ForgotPasswordEmailEvent event =
                ForgotPasswordEmailEvent.builder()
                        .email(user.getEmail())
                        .fullName(user.getFullName())
                        .token(token)
                        .expiresAt(expiresAt)
                        .build();
//       emailProducer.publishForgotPassword(event);
        eventPublisher.publishEvent(event);


    }



    public void resetPassword(ResetPasswordRequest request) {
        PasswordResetToken token = tokenRepository.findByToken(request.getToken())
                .orElseThrow(() -> new RuntimeException("Invalid token"));

        if (token.isUsed() || token.getExpiresAt().isBefore(Instant.now())) {
            throw new RuntimeException("Token expired or already used");
        }

        User user = token.getUser();
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        // Hủy hoặc xóa refreshToken khi reset password
        user.setRefreshToken(null);
        userRepository.save(user);

        token.setUsed(true);
        tokenRepository.save(token);
    }

    private String generate8CharToken() {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        SecureRandom random = new SecureRandom();
        StringBuilder sb = new StringBuilder(8);

        for (int i = 0; i < 8; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        return sb.toString();
    }

    @Transactional
    public ResponseEntity<?> forgotPassword(ForgotPasswordRequest request) {
        User user = userService.getbyEmail(request.getEmail());
        forgotPassword(user);
        ApiResponse<?> apiResponse = new ApiResponse<>(
                HttpStatus.OK.value(), null,
                "Password reset link has been sent to your email.", null);
        return ResponseEntity.ok(apiResponse);
    }

    @Transactional
    public ResponseEntity<?> reset_Password(ResetPasswordRequest request) {
        resetPassword(request);
        ApiResponse<?> apiResponse = new ApiResponse<>(
                HttpStatus.OK.value(), null,
                "Password has been successfully reset.", null);
        return ResponseEntity.ok(apiResponse);
    }
//    feat(auth): implement forgot password flow with RabbitMQ async email delivery
//    - implement forgot password token generation and expiration
//- publish email event after transaction commit
//- configure RabbitMQ exchange, queues, bindings and DLQ
//- add RabbitMQ producer and consumers
//- add retry mechanism with exponential backoff
//- configure dead letter queue for failed email messages
//- send reset password email using template
//- support asynchronous email processing

}
