package com.webjob.application.Service;

import com.webjob.application.Model.Entity.PasswordResetToken;
import com.webjob.application.Model.Entity.User;
import com.webjob.application.Dto.Request.ResetPasswordRequest;
import com.webjob.application.Repository.PasswordResetTokenRepository;
import com.webjob.application.Repository.UserRepository;
import com.webjob.application.Service.SendEmail.EmailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class PasswordResetService {
    @Autowired
    private  UserService userService;
    @Autowired
    private  PasswordResetTokenRepository tokenRepository;
    @Autowired
    private  PasswordEncoder passwordEncoder;
    @Autowired
    private EmailService emailService;
    @Autowired
    private  UserRepository userRepository;


    @Async
    @Transactional
    public void forgotPassword(User user) {
        // Generate token
        // 🔁 Xoá token cũ nếu tồn tại
        tokenRepository.findByUser(user).ifPresent(tokenRepository::delete);

//        dat lai PasswordResetToken new
        String token = UUID.randomUUID().toString();
        Instant expiresAt = Instant.now().plusSeconds(60 * 10); // expires in 10 mints
        PasswordResetToken resetToken = new PasswordResetToken();
        resetToken.setToken(token);
        resetToken.setExpiresAt(expiresAt);
        resetToken.setUser(user);
        resetToken.setUsed(false);
        tokenRepository.save(resetToken);

        // Send email
        String resetLink = "https://webjob-client.com/reset-password?token=" + token;
        Map<String, Object> emailVars = new HashMap<>();
        emailVars.put("name", user.getFullName()); // hoặc user.getUsername()
        emailVars.put("resetLink", resetLink);
        emailVars.put("token",token);
        emailVars.put("username",user.getEmail());

        emailService.sendTemplateResetPassword(
                "Password Reset Request",
                "emails/password-reset",
                emailVars
        );
    }

    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        PasswordResetToken token = tokenRepository.findByToken(request.getToken())
                .orElseThrow(() -> new RuntimeException("Invalid token"));

        if (token.isUsed() || token.getExpiresAt().isBefore(Instant.now())) {
            throw new RuntimeException("Token expired or already used");
        }

        User user = token.getUser();
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        token.setUsed(true);
        tokenRepository.save(token);
    }
}
