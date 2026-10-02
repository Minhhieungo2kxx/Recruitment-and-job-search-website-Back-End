package com.webjob.application.messaging.producer;

import com.webjob.application.messaging.config.RabbitMQConfig;
import com.webjob.application.messaging.dto.EmailJobMessage;
import com.webjob.application.messaging.dto.ForgotPasswordEmailEvent;
import com.webjob.application.messaging.dto.JobAlertMessage;
import com.webjob.application.messaging.dto.JobAppliedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailProducer {
    private final RabbitTemplate rabbitTemplate;

    public void publish(Long subscriberId) {

        EmailJobMessage message = EmailJobMessage
                .builder()
                .subscriberId(subscriberId)
                .build();

        rabbitTemplate.convertAndSend(
                RabbitMQConfig.EMAIL_EXCHANGE,
                RabbitMQConfig.EMAIL_ROUTING_KEY,
                message
        );

        log.info("Published subscriber {}", subscriberId);

    }
    @Retryable(
            retryFor = Exception.class,
            maxAttempts = 3,
            backoff = @Backoff(delay = 1000, multiplier = 2)
    )
    public void publishJobAlerts(Long jobAlertId) {
        try {
            JobAlertMessage message=JobAlertMessage.builder()
                    .jobAlertId(jobAlertId)
                    .build();
            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.EMAIL_EXCHANGE,
                    RabbitMQConfig.JOB_ALERT_ROUTING_KEY,
                    message
            );
            log.info("Published JobAlert {}",jobAlertId);
        }catch (Exception e){
            log.error("Failed to publish JobAlerts event {}", jobAlertId, e);
            throw e;
        }

    }


}
