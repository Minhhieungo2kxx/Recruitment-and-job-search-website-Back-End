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

import java.security.SecureRandom;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Locale;
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
        sendResetPasswordEmail(user,token,expiresAt);


    }
    public void sendResetPasswordEmail(User user, String token, Instant expiresAt) {
        // Tạo link reset
        String resetLink = "https://webjob-client.com/reset-password?token=" + token;

        // Chuẩn bị biến truyền vào email template
        Map<String, Object> emailVars = new HashMap<>();
        emailVars.put("name", user.getFullName());
        emailVars.put("username", user.getEmail());
        emailVars.put("resetLink", resetLink);
        emailVars.put("token", token);

        // Format thời gian theo giờ Việt Nam
        ZoneId userZone = ZoneId.of("Asia/Ho_Chi_Minh");
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(
                "HH:mm 'ngày' dd 'tháng' MM, yyyy (z)",
                new Locale("vi", "VN")
        ).withZone(userZone);

        // Chuyển expiry sang timezone VN
        ZonedDateTime expiryInUserZone = expiresAt.atZone(userZone);
        emailVars.put("expiryTime", formatter.format(expiryInUserZone));

        // Gửi email
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

}
