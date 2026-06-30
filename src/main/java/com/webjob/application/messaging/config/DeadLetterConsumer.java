package com.webjob.application.messaging.config;

import com.webjob.application.messaging.dto.EmailJobMessage;
import com.webjob.application.messaging.dto.ForgotPasswordEmailEvent;
import com.webjob.application.messaging.dto.JobAppliedEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class DeadLetterConsumer {

    @RabbitListener(queues = RabbitMQConfig.DLQ_QUEUE,
            containerFactory = "rabbitListenerContainerFactory")
    public void receive(EmailJobMessage message){

        log.error("Dead Letter Queue {}",message.getSubscriberId());

    }
    @RabbitListener(queues = RabbitMQConfig.FORGOT_DLQ,
            containerFactory = "rabbitListenerContainerFactory")
    public void receive(ForgotPasswordEmailEvent event){

        log.error("""
            Forgot Password Email moved to DLQ
            Email      : {}
            Token      : {}
            Expired At : {}
            """,
                event.getEmail(),
                event.getToken(),
                event.getExpiresAt()
        );

    }
    @RabbitListener(queues = RabbitMQConfig.JOB_APPLY_DLQ,
            containerFactory = "rabbitListenerContainerFactory")
    public void receive(JobAppliedEvent event){

        log.error("""
           Job Applied Email moved to DLQ
            Username      : {}
            UsernameHR     : {}
            Expired At : {}
            CompanyName : {}
            JobName : {}
            
            
            """,
                event.getEmail(),
                event.getUsername(),
                event.getUsernameHR(),
                event.getCompanyName(),
                event.getJobName()


        );

    }
}
