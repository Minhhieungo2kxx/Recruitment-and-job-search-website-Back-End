package com.webjob.application.messaging.consumer;

import com.webjob.application.dto.Request.NotificationRequest;
import com.webjob.application.enums.NotificationType;
import com.webjob.application.event.dto.JobCreatedEvent;
import com.webjob.application.messaging.config.RabbitMQConfig;
import com.webjob.application.models.Entity.User;
import com.webjob.application.service.NotificationService;
import com.webjob.application.utils.common.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class JobConsumer {
    private final NotificationService notificationService;
    private final SecurityUtils securityUtils;

    @RabbitListener(queues = RabbitMQConfig.FOLLOW_COMPANY_JOB_QUEUE,
            containerFactory = "rabbitListenerContainerFactory")
    public void receive(JobCreatedEvent message) {
        log.info("Received JobCreatedEvent from queue for jobId: {} and target userId: {}",
                message.getJobId(), message.getUserId());
        try {
            User user = securityUtils.getUserId(message.getUserId());
            if (user == null) {
                log.warn("User not found with id: {}. Skipping notification creation for jobId: {}",
                        message.getUserId(), message.getJobId());
                return;
            }
            String title = "Công ty bạn theo dõi vừa đăng tuyển";
            String content = """
                    %s vừa đăng tin tuyển dụng mới:
                    "%s"
                    Địa điểm: %s
                    Mức lương: %,.0f - %,.0f VNĐ
                    """
                    .formatted(
                            message.getCompanyName(),
                            message.getJobName(),
                            message.getLocal(),
                            message.getSalaryMin(),
                            message.getSalaryMax()
                    );

            NotificationRequest request = NotificationRequest.builder()
                    .user(user).title(title)
                    .content(content).type(NotificationType.COMPANY)
                    .referenceId(message.getJobId())
                    .redirectUrl("www.webJob/api/v1/jos/detail/" + message.getJobId())
                    .build();
            notificationService.createNotification(request);

            log.debug("Successfully created notification for userId: {} and jobId: {}",
                    message.getUserId(), message.getJobId());

        } catch (Exception e) {
            log.error("Failed to process received JobCreatedEvent for jobId: {} and userId: {}. Error: {}",
                    message.getJobId(), message.getUserId(), e.getMessage(), e);
            throw new RuntimeException(e.getMessage());
        }
    }
}
