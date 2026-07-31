package com.webjob.application.messaging.producer;

import com.webjob.application.event.dto.JobCreatedEvent;
import com.webjob.application.messaging.config.RabbitMQConfig;
import com.webjob.application.models.Entity.FollowCompany;
import com.webjob.application.repository.FollowCompanyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class JobProducer {

    private final RabbitTemplate rabbitTemplate;
    private final FollowCompanyRepository followCompanyRepository;

    public void sendJobCreated(JobCreatedEvent event) {
        log.info("Fetching followers with notification enabled for companyId: {}", event.getCompanyId());

        List<FollowCompany> followers = followCompanyRepository
                .findFollowersWithNotification(event.getCompanyId());

        if (followers.isEmpty()) {
            log.info("No followers found with notifications enabled for companyId: {}", event.getCompanyId());
            return;
        }

        log.info("Preparing to send job creation notifications to {} followers for jobId: {}",
                followers.size(), event.getJobId());

        int successCount = 0;
        int failCount = 0;

        for (FollowCompany follow : followers) {
            try {
                JobCreatedEvent jobCreatedEvent = JobCreatedEvent.builder()
                        .companyName(event.getCompanyName())
                        .companyId(event.getCompanyId())
                        .local(event.getLocal())
                        .SalaryMin(event.getSalaryMin())
                        .SalaryMax(event.getSalaryMax())
                        .userId(follow.getUser().getId())
                        .jobName(event.getJobName())
                        .jobId(event.getJobId())
                        .build();

                rabbitTemplate.convertAndSend(
                        RabbitMQConfig.FOLLOW_COMPANY_EXCHANGE,
                        RabbitMQConfig.FOLLOW_COMPANY_JOB_ROUTING_KEY,
                        jobCreatedEvent
                );
                successCount++;

            } catch (Exception e) {
                failCount++;

                log.error("Failed to send notification for jobId: {} to userId: {}. Error: {}",
                        event.getJobId(), follow.getUser().getId(), e.getMessage(), e);
            }
        }
        // Log tổng kết kết quả bắn message
        log.info("Finished sending job notifications for jobId: {}. Total success: {}, Total failed: {}",
                event.getJobId(), successCount, failCount);
    }

}
