package com.webjob.application.messaging.producer;

import com.webjob.application.messaging.config.RabbitMQConfig;
import com.webjob.application.messaging.dto.EmailJobMessage;
import com.webjob.application.messaging.dto.ForgotPasswordEmailEvent;
import com.webjob.application.messaging.dto.JobAlertMessage;
import com.webjob.application.messaging.dto.JobAppliedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
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
    public void publishJobAlerts(Long jobAlertId) {
        JobAlertMessage message=JobAlertMessage.builder()
                .jobAlertId(jobAlertId)
                .build();
        rabbitTemplate.convertAndSend(
                RabbitMQConfig.EMAIL_EXCHANGE,
                RabbitMQConfig.JOB_ALERT_ROUTING_KEY,
                message
        );
        log.info("Published JobAlert {}",jobAlertId);
    }
    @TransactionalEventListener(
            phase = TransactionPhase.AFTER_COMMIT
    )
    public void publishForgotPassword(ForgotPasswordEmailEvent event){

        rabbitTemplate.convertAndSend(
                RabbitMQConfig.EMAIL_EXCHANGE,
                RabbitMQConfig.FORGOT_ROUTING_KEY,
                event
        );
        log.info("Published email forgets {}", event.getEmail());

    }
    @TransactionalEventListener(
            phase = TransactionPhase.AFTER_COMMIT
    )
    public void handle(JobAppliedEvent event){
        rabbitTemplate.convertAndSend(
                RabbitMQConfig.EMAIL_EXCHANGE,
                RabbitMQConfig.JOB_APPLY_ROUTING_KEY,
                event
        );
        log.info("Published email Job Applied Event {}", event.getEmail());

    }

}
