package com.webjob.application.Service.SendEmail;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import java.nio.charset.StandardCharsets;
import java.util.Map;

@Service
public class EmailService {
    @Autowired
    private JavaMailSender mailSender;
    @Autowired
    private SpringTemplateEngine templateEngine;

    public void sendEmail(String to, String subject, String content, boolean isMultipart, boolean isHtml) {
        MimeMessage mimeMessage = mailSender.createMimeMessage();

        try {
            MimeMessageHelper helper = new MimeMessageHelper(
                    mimeMessage,
                    isMultipart,
                    StandardCharsets.UTF_8.name()
            );

            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(content, isHtml);

            mailSender.send(mimeMessage);

        } catch (MailException | MessagingException e) {
            throw new IllegalArgumentException(e.getMessage());
        }
    }
    public void sendEmailText(String to, String subject, String body) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(body, true);
            mailSender.send(message);
        } catch (MessagingException e) {
            throw new RuntimeException("Failed to send email", e);
        }
    }
    public void sendTemplateEmail(String to, String subject, String templateName, String name, Object jobList) {
        String content = generateEmailContent(templateName, name, jobList);
        sendEmail(to, subject, content, false, true);
    }
    private String generateEmailContent(String templateName, String name, Object jobList) {
        Context context = new Context();
        context.setVariable("name", name);
        context.setVariable("listjob", jobList);
        return templateEngine.process(templateName, context);
    }
    public void sendTemplateResetPassword(String subject, String templateName,Map<String, Object> variables) {
        String content =generate(templateName,variables);
        sendEmail((String) variables.get("username"), subject, content, false, true);
    }



    public void sendTemplateJobapply(String subject, String templateName, Map<String, Object> variables) {
        String content = generate(templateName, variables);
        sendEmail((String) variables.get("email"), subject, content, false, true);
    }


    public void sendLoginNotification(String subject, String templateName, Map<String, Object> variables) {
        String content = generate(templateName, variables);
        sendEmail((String) variables.get("email"), subject, content, false, true);
    }
    public void sendPaymentNotification(String subject, String templateName, Map<String, Object> variables) {
        String content = generate(templateName, variables);
        sendEmail((String) variables.get("email"), subject, content, false, true);
    }


    private String generate(String templateName,Map<String, Object> variables) {
        Context context = new Context();
        context.setVariables(variables);
        return templateEngine.process(templateName, context);
    }








}
