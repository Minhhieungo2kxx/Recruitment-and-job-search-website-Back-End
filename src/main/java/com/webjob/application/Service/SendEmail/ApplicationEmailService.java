package com.webjob.application.Service.SendEmail;


import com.webjob.application.Dto.Response.RespondEmailJob;
import com.webjob.application.Model.Entity.Company;
import com.webjob.application.Model.Entity.Job;
import com.webjob.application.Model.Entity.Subscriber;
import com.webjob.application.Model.Entity.User;
import com.webjob.application.Service.SubscriberService;
import eu.bitwalker.useragentutils.DeviceType;
import eu.bitwalker.useragentutils.UserAgent;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class ApplicationEmailService {

    private final EmailService emailService;


    public ApplicationEmailService(EmailService emailService) {
        this.emailService = emailService;

    }

    @Async("taskExecutor")
    public void sendJobApplicate(User user, Job job,User hr) {
        Company company = job.getCompany();

        Map<String, Object> emailVars = new HashMap<>();
        emailVars.put("email", user.getEmail());
        emailVars.put("username", user.getFullName());
        emailVars.put("usernameHR",hr.getFullName());
        emailVars.put("namecompany",company.getName());
        emailVars.put("nameJob", job.getName());
        emailVars.put("logo",company.getLogo());
        http://localhost:8081/storage/company/
        emailVars.put("salary", formatVietnameseCurrency(job.getSalary()));
        emailVars.put("location", job.getLocation());

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
                .withZone(ZoneId.of("Asia/Ho_Chi_Minh"));
        String startDateFormatted = formatter.format(job.getStartDate());
        String endDateFormatted = formatter.format(job.getEndDate());
        emailVars.put("starttime", startDateFormatted);
        emailVars.put("endtime", endDateFormatted);

        emailService.sendTemplateJobapply(
                "Thông tin Job vừa ứng tuyển",
                "emails/emailjob-apply",
                emailVars
        );
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

    @Async("taskExecutor")
    public void sendJobEmail(Subscriber subscriber, List<Job> jobs) {
        List<RespondEmailJob> jobSummaries = Optional.ofNullable(jobs)
                .orElseGet(Collections::emptyList)  // nếu jobs null → dùng list rỗng
                .stream()
                .map(this::convertJobToSendEmail)
                .filter(Objects::nonNull)          // loại bỏ các kết quả null
                .collect(Collectors.toList());      // Java 8
        try {
            emailService.sendTemplateEmail(
                    subscriber.getEmail(),
                    "Cơ hội việc làm hot đang chờ bạn!",
                    "emails/job",
                    subscriber.getName(),
                    jobSummaries
            );
            log.info("Sent job email to {}", subscriber.getEmail());
        } catch (Exception e) {
            log.error("Failed to send email to {}", subscriber.getEmail(), e);
        }
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
        try {
            String url = "http://ip-api.com/json/" + ip;
            RestTemplate rest = new RestTemplate();
            Map<String, Object> response = rest.getForObject(url, Map.class);

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

    private String formatVietnameseCurrency(double amount) {
        NumberFormat vndFormat = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));
        return vndFormat.format(amount);
    }

}
