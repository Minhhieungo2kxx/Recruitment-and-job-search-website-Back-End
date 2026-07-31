package com.webjob.application.messaging.config;

import com.webjob.application.event.dto.JobCreatedEvent;
import com.webjob.application.messaging.dto.EmailJobMessage;
import com.webjob.application.messaging.dto.ForgotPasswordEmailEvent;
import com.webjob.application.messaging.dto.JobAlertMessage;
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
    @RabbitListener(queues = RabbitMQConfig.JOB_ALERT_DLQ,
            containerFactory = "rabbitListenerContainerFactory")
    public void receive(JobAlertMessage message){
        log.error("Dead Letter Queue {}",message.getJobAlertId());
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
            CompanyName : {}
            JobName : {}
            
            """,
                event.getCandidateName(),
                event.getHrName(),
                event.getCompanyName(),
                event.getJobName()


        );

    }
    @RabbitListener(queues = RabbitMQConfig.FOLLOW_COMPANY_JOB_DLQ,
            containerFactory = "rabbitListenerContainerFactory")
    public void receive(JobCreatedEvent event) {
        log.error("""
                JobCreated Notification moved to DLQ
                jobName     : {}
                companyName : {}
                UserID      : {}
                """,
                event.getJobName(),
                event.getCompanyName(),
                event.getUserId()
        );
    }
}
