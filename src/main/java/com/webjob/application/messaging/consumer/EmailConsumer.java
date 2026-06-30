package com.webjob.application.messaging.consumer;

import com.webjob.application.messaging.config.RabbitMQConfig;
import com.webjob.application.messaging.dto.EmailJobMessage;
import com.webjob.application.messaging.dto.ForgotPasswordEmailEvent;
import com.webjob.application.messaging.dto.JobAppliedEvent;
import com.webjob.application.models.Entity.Job;
import com.webjob.application.models.Entity.Skill;
import com.webjob.application.models.Entity.Subscriber;
import com.webjob.application.repository.JobRepository;
import com.webjob.application.repository.SubscriberRepository;
import com.webjob.application.service.SendEmail.ApplicationEmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Component
@Slf4j
@RequiredArgsConstructor
public class EmailConsumer {
    private final SubscriberRepository subscriberRepository;

    private final JobRepository jobRepository;

    private final ApplicationEmailService applicationEmailService;
    @RabbitListener(
            queues = RabbitMQConfig.EMAIL_QUEUE,
            containerFactory = "rabbitListenerContainerFactory"
    )
    public void receive(EmailJobMessage message) {

        Long subscriberId = message.getSubscriberId();

        Subscriber subscriber = subscriberRepository
                .findWithSkillsById(subscriberId)
                .orElseThrow(() ->
                        new RuntimeException("Subscriber not found " +subscriberId));

        List<Skill> skills =
                Optional.ofNullable(subscriber.getSkills())
                        .orElse(Collections.emptyList());

        if (skills.isEmpty()) {

            log.info("Subscriber {} has no skill", subscriberId);

            return;
        }

        List<Job> jobs =
                jobRepository.findTop10BySkills(
                        skills,
                        PageRequest.of(0,10));

        if (jobs.isEmpty()) {

            log.info("No job found {}", subscriberId);

            return;
        }

        applicationEmailService.sendJobEmail(subscriber,jobs);

    }
    @RabbitListener(
            queues = RabbitMQConfig.FORGOT_QUEUE,
            containerFactory = "rabbitListenerContainerFactory"
    )
    public void receive(ForgotPasswordEmailEvent event){
        applicationEmailService.sendResetPasswordEmail(
                event.getEmail(),
                event.getFullName(),
                event.getToken(),
                event.getExpiresAt()
        );
        log.info("Email consumer forget password: {} ",event.getEmail());


    }
    @RabbitListener(queues = RabbitMQConfig.JOB_APPLY_QUEUE,
            containerFactory = "rabbitListenerContainerFactory")
    public void receive(JobAppliedEvent event){
        applicationEmailService.sendJobApplicate(event);
        log.info("Job Applied sent information email {} ",event.getEmail());

    }


}
