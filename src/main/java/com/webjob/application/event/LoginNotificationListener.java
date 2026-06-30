package com.webjob.application.event;

import com.webjob.application.dto.record.LoginSuccessEvent;
import com.webjob.application.service.SendEmail.ApplicationEmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class LoginNotificationListener {
    private final ApplicationEmailService applicationEmailService;


    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleLoginSuccess(
            LoginSuccessEvent event
    ) {
        Map<String, Object> emailVars = new HashMap<>();

        emailVars.put("email", event.email());
        emailVars.put("ip", event.ip());
        emailVars.put("userAgent", event.userAgent());
        emailVars.put("time", event.loginTime().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")));
        applicationEmailService.LoginNotification(emailVars);
    }
}
