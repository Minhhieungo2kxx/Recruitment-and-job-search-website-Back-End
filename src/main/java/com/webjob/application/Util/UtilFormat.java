package com.webjob.application.Util;

import lombok.*;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.text.NumberFormat;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;


@AllArgsConstructor
@Setter
@Getter
@Builder
public class UtilFormat {
    public static final ZoneId VIETNAM_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    public static final DateTimeFormatter YYYYMMDD_HHMMSS =
            DateTimeFormatter.ofPattern("yyyyMMddHHmmss")
                    .withZone(VIETNAM_ZONE);
    public static String formatAmount(double amount) {
        NumberFormat formatter = NumberFormat.getInstance(new Locale("vi", "VN"));
        return formatter.format(amount) + " VNĐ";
    }

    public static String formatTime(Instant time) {
        return DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")
                .withZone(VIETNAM_ZONE)
                .format(time);
    }
    public static Instant parseToInstant(String dateTimeStr) {
        return LocalDateTime
                .parse(dateTimeStr, YYYYMMDD_HHMMSS)
                .atZone(VIETNAM_ZONE)
                .toInstant();
    }

    public static String generate8CharToken() {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        SecureRandom random = new SecureRandom();
        StringBuilder sb = new StringBuilder(8);

        for (int i = 0; i < 8; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        return sb.toString();
    }




}
