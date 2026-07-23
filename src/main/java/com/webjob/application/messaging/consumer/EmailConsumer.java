package com.webjob.application.messaging.consumer;

import com.webjob.application.enums.JobStatus;
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
    @RabbitListener(
            queues = RabbitMQConfig.EMAIL_QUEUE,
            containerFactory = "rabbitListenerContainerFactory"
    )
    @Transactional
    public void receive(EmailJobMessage message) {
        Long subscriberId = message.getSubscriberId();

        Subscriber subscriber = subscriberRepository.findSubscriberDetail(subscriberId)
                .orElseThrow(() ->new ResourceNotFoundException("Subscriber not found " +subscriberId));

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
                ,subscriber.getLastCheckedAt(),PageRequest.of(0,10));

        if (jobs.isEmpty()) {
            log.info("No job found {}", subscriberId);
            return;
        }
        applicationEmailService.sendJobEmail(subscriber,jobs);
        subscriber.setLastCheckedAt(Instant.now());
        subscriberRepository.save(subscriber);
    }

    @RabbitListener(
            queues = RabbitMQConfig.JOB_ALERT_QUEUE,
            containerFactory = "rabbitListenerContainerFactory"
    )
    @Transactional
    public void receive(JobAlertMessage message) {
        Long jobAlertId = message.getJobAlertId();

        JobAlert jobAlert = jobAlertRepository.findById(jobAlertId)
                .orElseThrow(() ->new ResourceNotFoundException("Subscriber not found " + jobAlertId));
        Pageable limit10 = PageRequest.of(0,10);
        List<Job> findRecommendedJobs=jobRepository.findTopJobsForAlert(
                jobAlert.getKeyword(),jobAlert.getLocation()
                ,jobAlert.getJobCategory().getId(),jobAlert.getLevel()
                ,jobAlert.getWorkMode(),jobAlert.getSalaryMin()
                ,jobAlert.getSalaryMax(),jobAlert.getWorkingType(),limit10
        );
        if (findRecommendedJobs.isEmpty()) {
            log.info("No job found {}", jobAlertId);
            return;
        }
        applicationEmailService.sendJobAlertEmail(jobAlert,findRecommendedJobs);
        jobAlert.setNextRunAt(calculateNextRunAt(jobAlert));
        jobAlert.setLastCheckedAt(Instant.now());
        jobAlertRepository.save(jobAlert);

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


    private Instant calculateNextRunAt(JobAlert jobAlert) {
        return switch (jobAlert.getFrequency()) {
            case IMMEDIATELY -> Instant.now();
            case DAILY -> jobAlert.getNextRunAt().plus(1, ChronoUnit.DAYS);
            case WEEKLY -> jobAlert.getNextRunAt().plus(7, ChronoUnit.DAYS);
        };
    }


}
