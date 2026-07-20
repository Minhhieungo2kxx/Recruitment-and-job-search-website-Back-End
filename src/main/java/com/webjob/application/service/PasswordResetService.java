package com.webjob.application.service;

import com.webjob.application.dto.Request.ForgotPasswordRequest;
import com.webjob.application.dto.Response.ApiResponse;
import com.webjob.application.exception.Customs.BadRequestException;
import com.webjob.application.exception.Customs.ResourceNotFoundException;
import com.webjob.application.messaging.dto.ForgotPasswordEmailEvent;
import com.webjob.application.messaging.producer.EmailProducer;
import com.webjob.application.models.Entity.User;
import com.webjob.application.dto.Request.ResetPasswordRequest;
import com.webjob.application.repository.UserRepository;
import com.webjob.application.service.Redis.PasswordResetRedisService;
import com.webjob.application.service.SendEmail.EmailService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PasswordResetService {

    private final UserRepository userRepository;


    private final PasswordEncoder passwordEncoder;

    private final EmailService emailService;

    private final EmailProducer emailProducer;
    private final ApplicationEventPublisher eventPublisher;

    private final PasswordResetRedisService redisService;

    private String generateToken() {

        byte[] bytes = new byte[32];

        new SecureRandom().nextBytes(bytes);

        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(bytes);
    }


    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        Long userId = redisService.getUserId(request.getToken());
        if (userId == null) {
            throw new BadRequestException("Token invalid or expired");
        }
        User user = userRepository.findById(userId).orElseThrow(
                () -> new  ResourceNotFoundException("User not found or no Active  with id: " + userId)
        );

        if (!passwordEncoder.matches(request.getOldPassword(), user.getPassword())) {
            throw new BadRequestException("Mật khẩu cũ không đúng");
        }

        if (!request.getNewPassword().equals(request.getConfirmNewPassword())) {
            throw new BadRequestException("Xác nhận mật khẩu không khớp");
        }

        if (request.getOldPassword().equals(request.getNewPassword())) {
            throw new BadRequestException("Mật khẩu mới không được trùng với mật khẩu cũ");
        }
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        // Hủy hoặc xóa refreshToken khi reset password
        user.setRefreshToken(null);
        redisService.delete(userId,request.getToken());
        userRepository.save(user);

    }


    @Transactional
    public void forgotPassword(ForgotPasswordRequest request) {
        User user = userRepository.findByEmail(request.getEmail());
        String token = generateToken();

        redisService.save(user.getId(), token, Duration.ofMinutes(6));


//        send Email
        ForgotPasswordEmailEvent event =
                ForgotPasswordEmailEvent.builder()
                        .email(user.getEmail())
                        .fullName(user.getFullName())
                        .token(token)
                        .expiresAt(Instant.now().plusSeconds(360))
                        .build();

        eventPublisher.publishEvent(event);
    }





}
