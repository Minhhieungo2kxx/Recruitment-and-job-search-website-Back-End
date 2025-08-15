package com.webjob.application.Services.SendEmail;


import com.webjob.application.Models.Entity.Company;
import com.webjob.application.Models.Entity.Job;
import com.webjob.application.Models.Entity.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.text.NumberFormat;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

@Service
public class ApplicationEmailService {
    @Autowired
    private EmailService emailService;

    @Async("taskExecutor")
    public void sendJobApplicate(User user, Job job) {
        Company company = job.getCompany();

        Map<String, Object> emailVars = new HashMap<>();
        emailVars.put("email", user.getEmail());
        emailVars.put("username", user.getFullName());
        emailVars.put("namecompany", company.getName());
        emailVars.put("nameJob", job.getName());
        emailVars.put("logo", company.getLogo());
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
                "emailjob-apply",
                emailVars
        );
    }

    private String formatVietnameseCurrency(double amount) {
        NumberFormat vndFormat = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));
        return vndFormat.format(amount);
    }
}
