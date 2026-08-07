package com.webjob.application.utils.common;

import lombok.*;

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



    public static String extractOkContentForHistory(String aiResponse) {
        if (aiResponse == null) {
            return "";
        }
        return aiResponse
                .replaceFirst("^\\[OK\\]\\s*", "")
                .trim();
    }

    public static String extractOkContentForUser(String aiResponse) {
        return hideInternalIds(extractOkContentForHistory(aiResponse));
    }
    public static String hideInternalIds(String content) {
        if (content == null) {
            return "";
        }
        return content
                .replaceAll("\\[JOB_ID:\\d+\\]\\s*", "")
                .replaceAll("\\[COMPANY_ID:\\d+\\]\\s*", "")
                .trim();
    }
    public static String formatForUser(String content) {
        if (content == null) {
            return "";
        }

        return content
                .replaceFirst("^\\[OK\\]\\s*", "")
                .replaceAll("\\[JOB_ID:\\d+\\]\\s*", "")
                .replaceAll("\\[COMPANY_ID:\\d+\\]\\s*", "")
                .trim();
    }

    public static String asString(Object o) {
        return o == null ? null : o.toString();
    }

    public static Long asLong(Object o) {
        return o == null ? null : (long) Double.parseDouble(o.toString());
    }

    public static Integer asInteger(Object o) {
        return o == null ? null : (int) Double.parseDouble(o.toString());
    }

    public static Double asDouble(Object o) {
        return o == null ? null : Double.parseDouble(o.toString());
    }



}
