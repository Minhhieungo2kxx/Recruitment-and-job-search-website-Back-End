package com.webjob.application.messaging.consumer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.webjob.application.document.JobDocument;
import com.webjob.application.dto.Response.RabbitEvent;
import com.webjob.application.enums.JobStatus;
import com.webjob.application.enums.OutboxEventType;
import com.webjob.application.exception.Customs.ResourceNotFoundException;
import com.webjob.application.messaging.config.RabbitMQConfig;
import com.webjob.application.messaging.dto.EmailJobMessage;

import com.webjob.application.messaging.dto.ForgotPasswordEmailEvent;
import com.webjob.application.messaging.dto.JobAlertMessage;
import com.webjob.application.messaging.dto.JobAppliedEvent;
import com.webjob.application.models.Entity.*;
import com.webjob.application.repository.JobAlertRepository;
import com.webjob.application.repository.JobRepository;
import com.webjob.application.repository.SubscriberRepository;
import com.webjob.application.service.OutBox.RabbitMessageDeupService;
import com.webjob.application.service.SendEmail.ApplicationEmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Component
@Slf4j
@RequiredArgsConstructor
public class EmailConsumer {
    private final SubscriberRepository subscriberRepository;

    private final JobRepository jobRepository;

    private final JobAlertRepository jobAlertRepository;

    private final ApplicationEmailService applicationEmailService;

    private final RabbitMessageDeupService rabbitMessageDedupService;

    private final ObjectMapper objectMapper;

    @RabbitListener(
            queues = RabbitMQConfig.EMAIL_QUEUE,
            containerFactory = "rabbitListenerContainerFactory"
    )
    @Transactional
    public void receive(EmailJobMessage message) {
        try {
            Long subscriberId = message.getSubscriberId();

            Subscriber subscriber = subscriberRepository.findSubscriberDetail(subscriberId)
                    .orElseThrow(() -> new ResourceNotFoundException("Subscriber not found " + subscriberId));

            List<Skill> skills = Optional.of(subscriber.getSubscriberSkills()
                            .stream()
                            .map(SubscriberSkill::getSkill)
                            .toList())
                    .orElseGet(Collections::emptyList);

            if (skills.isEmpty()) {
                log.info("Subscriber {} has no skill", subscriberId);
                return;
            }

            List<Job> jobs = jobRepository.findTop10BySkills(skills, Instant.now()
                    , subscriber.getLastCheckedAt(), PageRequest.of(0, 10));

            if (jobs.isEmpty()) {
                log.info("No job found {}", subscriberId);
                return;
            }
            applicationEmailService.sendJobEmail(subscriber, jobs);
            subscriber.setLastCheckedAt(Instant.now());
            subscriberRepository.save(subscriber);

        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }

    @RabbitListener(
            queues = RabbitMQConfig.JOB_ALERT_QUEUE,
            containerFactory = "rabbitListenerContainerFactory"
    )
    @Transactional
    public void receive(JobAlertMessage message) {
        try {
            Long jobAlertId = message.getJobAlertId();

            JobAlert jobAlert = jobAlertRepository.findById(jobAlertId)
                    .orElseThrow(() -> new ResourceNotFoundException("Subscriber not found " + jobAlertId));
            Pageable limit10 = PageRequest.of(0, 10);
            List<Job> findRecommendedJobs = jobRepository.findTopJobsForAlert(
                    jobAlert.getKeyword(), jobAlert.getLocation()
                    , jobAlert.getJobCategory().getId(), jobAlert.getLevel()
                    , jobAlert.getWorkMode(), jobAlert.getSalaryMin()
                    , jobAlert.getSalaryMax(), jobAlert.getWorkingType(), limit10
            );
            if (findRecommendedJobs.isEmpty()) {
                log.info("No job found {}", jobAlertId);
                return;
            }
            applicationEmailService.sendJobAlertEmail(jobAlert, findRecommendedJobs);
            jobAlert.setNextRunAt(calculateNextRunAt(jobAlert));
            jobAlert.setLastCheckedAt(Instant.now());
            jobAlertRepository.save(jobAlert);

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


    @RabbitListener(
            queues = RabbitMQConfig.FORGOT_QUEUE,
            containerFactory = "rabbitListenerContainerFactory"
    )
    public void receive(RabbitEvent<String> event) {
        String queueName = RabbitMQConfig.FORGOT_QUEUE;
        String eventId = event.getEventId();
        boolean acquired = rabbitMessageDedupService.tryStartProcessing(queueName, eventId
        );
        if (!acquired) {
            log.info("Duplicate/in-flight event ignored. eventId={}, eventType={}",
                    eventId, event.getEventType());
            return;
        }
        try {
            processEventForgotpassword(event);
            rabbitMessageDedupService.markProcessed(queueName, eventId);
        } catch (Exception e) {
            rabbitMessageDedupService.removeProcessing(queueName, eventId);
            throw new RuntimeException(e);
        }
    }

    private void processEventForgotpassword(RabbitEvent<String> event) {

        if (OutboxEventType.FORGOT_PASSWORD.name().equals(event.getEventType())) {

            ForgotPasswordEmailEvent forgot;
            try {
                forgot = objectMapper.readValue(event.getPayload(), ForgotPasswordEmailEvent.class);
            } catch (JsonProcessingException e) {
                throw new RuntimeException("Lỗi khi parse ForgotPasswordEmailEvent từ payload", e);
            }

            applicationEmailService.sendResetPasswordEmail(
                    forgot.getEmail(),
                    forgot.getFullName(),
                    forgot.getToken(),
                    forgot.getExpiresAt()
            );


        }
    }


    @RabbitListener(queues = RabbitMQConfig.JOB_APPLY_QUEUE,
            containerFactory = "rabbitListenerContainerFactory")
    public void receiveJobApply(RabbitEvent<String> event) {
        String queueName = RabbitMQConfig.JOB_APPLY_QUEUE;
        String eventId = event.getEventId();
        boolean acquired = rabbitMessageDedupService.tryStartProcessing(queueName, eventId
        );
        if (!acquired) {
            log.info("Duplicate/in-flight event ignored. eventId={}, eventType={}",
                    eventId, event.getEventType());
            return;
        }
        try {
            processEventJobApply(event);
            rabbitMessageDedupService.markProcessed(queueName, eventId);
        } catch (Exception e) {
            rabbitMessageDedupService.removeProcessing(queueName, eventId);
            throw new RuntimeException(e);
        }
    }

    private void processEventJobApply(RabbitEvent<String> event) {

        if (OutboxEventType.JOB_APPLY.name().equals(event.getEventType())) {

            JobAppliedEvent jobAppliedEvent;
            try {
                jobAppliedEvent = objectMapper.readValue(event.getPayload(), JobAppliedEvent.class);
            } catch (JsonProcessingException e) {
                throw new RuntimeException("Lỗi khi parse ForgotPasswordEmailEvent từ payload", e);
            }
            applicationEmailService.sendJobApplicate(jobAppliedEvent);

        }
    }


    private Instant calculateNextRunAt(JobAlert jobAlert) {
        return switch (jobAlert.getFrequency()) {
            case IMMEDIATELY -> Instant.now();
            case DAILY -> jobAlert.getNextRunAt().plus(1, ChronoUnit.DAYS);
            case WEEKLY -> jobAlert.getNextRunAt().plus(7, ChronoUnit.DAYS);
        };
    }


}
