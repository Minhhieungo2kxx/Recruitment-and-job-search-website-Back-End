package com.webjob.application.service.SendEmail;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import java.nio.charset.StandardCharsets;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;

    private final SpringTemplateEngine templateEngine;

    @Value("${spring.mail.username}")
    private String fromEmail;


public void sendEmail(String to, String subject, String content) {

    MimeMessage mimeMessage = mailSender.createMimeMessage();

    try {

        MimeMessageHelper helper = new MimeMessageHelper(
                mimeMessage,
                true,
                StandardCharsets.UTF_8.name()
        );

        helper.setFrom(fromEmail);
        helper.setTo(to);
        helper.setSubject(subject);

        helper.setText(content, true);

        ClassPathResource logo = new ClassPathResource("static/images/logo.png");
        helper.addInline(
                "webjobLogo",
                logo
        );

        mailSender.send(mimeMessage);

        log.info("Send email success");

    } catch (Exception e) {
        log.error("Send email failed", e);
        throw new RuntimeException(e);
    }
}

    public void sendTemplateEmail(String to, String subject, String templateName, String name, Object jobList) {
        String content = generateEmailContent(templateName, name, jobList);
        sendEmail(to, subject, content);
    }
    private String generateEmailContent(String templateName, String name, Object jobList) {
        Context context = new Context();
        context.setVariable("name", name);
        context.setVariable("listjob", jobList);
        return templateEngine.process(templateName, context);
    }

    public void sendTemplateResetPassword(String subject, String templateName,Map<String, Object> variables) {
        String content =generate(templateName,variables);
        sendEmail((String) variables.get("username"), subject, content);
    }



    public void sendTemplateJobapply(String subject, String templateName, Map<String, Object> variables) {
        String content = generate(templateName, variables);
        sendEmail((String) variables.get("email"), subject, content);
    }


    public void sendLoginNotification(String subject, String templateName, Map<String, Object> variables) {
        String content = generate(templateName, variables);
        sendEmail((String) variables.get("email"), subject, content);
    }
    public void sendPaymentNotification(String subject, String templateName, Map<String, Object> variables) {
        String content = generate(templateName, variables);
        sendEmail((String) variables.get("email"), subject, content);

    }


    private String generate(String templateName,Map<String, Object> variables) {
        Context context = new Context();
        context.setVariables(variables);
        return templateEngine.process(templateName, context);
    }








}
