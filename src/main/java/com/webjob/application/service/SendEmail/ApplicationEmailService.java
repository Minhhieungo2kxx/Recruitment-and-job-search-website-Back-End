package com.webjob.application.service.SendEmail;


import com.webjob.application.dto.Request.PaymentSuccessDto;
import com.webjob.application.dto.Response.RespondEmailJob;
import com.webjob.application.messaging.dto.JobAppliedEvent;
import com.webjob.application.models.Entity.*;
import com.webjob.application.utils.UtilFormat;
import eu.bitwalker.useragentutils.DeviceType;
import eu.bitwalker.useragentutils.UserAgent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.text.NumberFormat;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class ApplicationEmailService {

    private final EmailService emailService;


//    @Async("taskExecutor")
    public void sendJobApplicate(JobAppliedEvent event) {

        Map<String, Object> emailVars = new HashMap<>();
        emailVars.put("email", event.getEmail());
        emailVars.put("username",event.getUsername());
        emailVars.put("usernameHR",event.getUsernameHR());
        emailVars.put("namecompany",event.getCompanyName());
        emailVars.put("nameJob", event.getJobName());
        emailVars.put("logo",event.getCompanyLogo());
        http://localhost:8081/storage/company/
        emailVars.put("salary", formatVietnameseCurrency(event.getSalary().doubleValue()));
        emailVars.put("location",event.getLocation());

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
                .withZone(ZoneId.of("Asia/Ho_Chi_Minh"));
        String startDateFormatted = formatter.format(event.getStartDate());
        String endDateFormatted = formatter.format(event.getEndDate());
        emailVars.put("starttime", startDateFormatted);
        emailVars.put("endtime", endDateFormatted);

        emailService.sendTemplateJobapply(
                "Thông tin Job vừa ứng tuyển",
                "emails/emailjob-apply",
                emailVars
        );
        log.info("Sent job email to {}", event.getEmail());
    }



    @Async("taskExecutor")
    public void LoginNotification(Map<String, Object> emailVars) {
        // Lấy dữ liệu từ map
        String email = (String) emailVars.get("email");
        String ip = (String) emailVars.get("ip");
        String userAgent = (String) emailVars.get("userAgent");

        Map<String, Object> emailTemplateVars = new HashMap<>();
        emailTemplateVars.put("email", email);
        emailTemplateVars.put("time", emailVars.get("time"));
        emailTemplateVars.put("location", getLocationByIp(ip));
        emailTemplateVars.put("device", getDeviceBasic(userAgent));

        emailService.sendLoginNotification(
                "Thông Báo Đăng Nhập Hệ Thống TopWork",
                "emails/login-notification",
                emailTemplateVars
        );
    }


    public void sendJobEmail(Subscriber subscriber, List<Job> jobs) {
        List<RespondEmailJob> jobSummaries = Optional.ofNullable(jobs)
                .orElseGet(Collections::emptyList)  // nếu jobs null → dùng list rỗng
                .stream()
                .map(this::convertJobToSendEmail)
                .filter(Objects::nonNull)          // loại bỏ các kết quả null
                .collect(Collectors.toList());      // Java 8

            emailService.sendTemplateEmail(
                    subscriber.getEmail(),
                    "Cơ hội việc làm hot đang chờ bạn!",
                    "emails/job",
                    subscriber.getName(),
                    jobSummaries
            );
            log.info("Sent job email to {}", subscriber.getEmail());

    }
    public void sendResetPasswordEmail(String email, String fullName, String token, Instant expiresAt) {
        // Tạo link reset
        String resetLink = "https://webjob-client.com/reset-password?token=" + token;

        // Chuẩn bị biến truyền vào email template
        Map<String, Object> emailVars = new HashMap<>();
        emailVars.put("name",fullName);
        emailVars.put("username",email);
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
        log.info("Sent job email to {}", email);
    }

    public RespondEmailJob convertJobToSendEmail(Job job) {
        if (job == null) {
            throw new IllegalArgumentException("Job must not be null");
        }
        String companyName = job.getCompany() != null ? job.getCompany().getName() : "Unknown";

        List<RespondEmailJob.SkillEmail> skillEmails = Optional.ofNullable(job.getSkills())
                .orElse(Collections.emptyList())
                .stream()
                .map(skill -> new RespondEmailJob.SkillEmail(skill.getName()))
                .collect(Collectors.toList());
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
                .withZone(ZoneId.of("Asia/Ho_Chi_Minh"));
        RespondEmailJob response = RespondEmailJob.builder()
                .name(job.getName())
                .formattedSalary(formatVietnameseCurrency(job.getSalary()))
                .company(new RespondEmailJob.CompanyEmail(companyName))
                .skills(skillEmails)
                .startDate(formatter.format(job.getStartDate()))
                .endDate(formatter.format(job.getEndDate()))
                .build();
        return response;
    }
    public String getLocationByIp(String ip) {

        // IP local / private → không gọi API
        if (ip == null ||
                ip.equals("127.0.0.1") ||
                ip.equals("0:0:0:0:0:0:0:1") ||
                ip.startsWith("192.168.") ||
                ip.startsWith("10.")) {

            return "Localhost";
        }

        try {
            String url = "http://ip-api.com/json/" + ip;
            RestTemplate rest = new RestTemplate();
            Map<String, Object> response = rest.getForObject(url, Map.class);

            if (response == null || !"success".equals(response.get("status"))) {
                return "Unknown";
            }

            return response.get("city") + ", " + response.get("country");

        } catch (Exception e) {
            return "Unknown";
        }
    }

    public String getDeviceBasic(String userAgentString) {
        if (userAgentString == null || userAgentString.isEmpty()) {
            return "Không xác định";
        }
        UserAgent userAgent = UserAgent.parseUserAgentString(userAgentString);
        // Lấy thông tin browser + os
        String browser = userAgent.getBrowser().getName();
        String os = userAgent.getOperatingSystem().getName();

        DeviceType type = userAgent.getOperatingSystem().getDeviceType();
        String deviceTypeVN;
        switch (type) {
            case MOBILE -> deviceTypeVN = "Điện thoại";
            case TABLET -> deviceTypeVN = "Máy tính bảng";
            case COMPUTER -> deviceTypeVN = "Laptop / Máy tính";
            case WEARABLE -> deviceTypeVN = "Thiết bị đeo tay";
            case GAME_CONSOLE -> deviceTypeVN = "Máy chơi game";
            default -> deviceTypeVN = "Không xác định";
        }

        return deviceTypeVN + " - " + os + " - " + browser;
    }
    @Async("taskExecutor")
    public void sendPaymentEmail(Payment payment) {
        Map<String, Object> vars = new HashMap<>();
        vars.put("email",payment.getUser().getEmail()); // cần có
        vars.put("userName",payment.getUser().getFullName());
        vars.put("jobName", payment.getJob().getName());
        vars.put("amount", UtilFormat.formatAmount(payment.getAmount()));
        vars.put("status", payment.getStatus());
        vars.put("transactionId",payment.getTransactionId());
        vars.put("payDate",UtilFormat.formatTime(payment.getPayDate()));
        vars.put("gateway", payment.getProvider());
        vars.put("bankCode", payment.getBankCode());
        vars.put("responseCode",payment.getResponseCode());
        vars.put("orderInfo",payment.getOrderInfo());
        vars.put("payType",payment.getPayType());
        vars.put("orderType",payment.getOrderType());

        emailService.sendPaymentNotification(
                "Xác Nhận Thanh Toán WebJob",
                "emails/payment-notification",
                vars
        );


    }
    @Async("taskExecutor")
    public void sendMomoPaymentEmail(PaymentSuccessDto dto ) {
        try {
            Map<String, Object> vars = new HashMap<>();
            vars.put("email",dto.getEmail());
            vars.put("userName",dto.getUserName());
            vars.put("jobName",dto.getJobName());
            vars.put("amount", dto.getAmount());
            vars.put("status",dto.getStatus());
            vars.put("transactionId",dto.getTransactionId());
            vars.put("payDate", dto.getPayDate());
            vars.put("gateway",dto.getGateway());
            vars.put("bankCode", dto.getBankCode());
            vars.put("responseCode",dto.getResponseCode());
            vars.put("orderInfo",dto.getOrderInfo());
            vars.put("payType",dto.getPayType());
            vars.put("orderType",dto.getOrderType());

            emailService.sendPaymentNotification(
                    "Xác Nhận Thanh Toán WebJob",
                    "emails/payment-notification",
                    vars);


        } catch (Exception e) {
            log.error("Send payment email failed", e);
        }



    }

    private String formatVietnameseCurrency(double amount) {
        NumberFormat vndFormat = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));
        return vndFormat.format(amount);
    }



}
