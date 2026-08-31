package com.webjob.application.event;

import com.webjob.application.dto.Request.JobRestoredEvent;
import com.webjob.application.dto.Request.NotificationRequest;
import com.webjob.application.dto.record.*;
import com.webjob.application.elasticsearch.job.JobIndexService;
import com.webjob.application.enums.NotificationType;
import com.webjob.application.event.dto.JobAppliedNotificationEvent;
import com.webjob.application.event.dto.JobCreatedEvent;
import com.webjob.application.exception.Customs.BadRequestException;
import com.webjob.application.messaging.producer.JobProducer;
import com.webjob.application.models.Entity.User;
import com.webjob.application.service.NotificationService;
import com.webjob.application.utils.common.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
@Slf4j
public class JobEventListener {
    private final JobProducer jobProducer;
    private final NotificationService notificationService;
    private final SecurityUtils securityUtils;
    private final JobIndexService jobIndexService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onJobCreated(JobCreatedEvent event) {
        log.info("Processing JobCreatedEvent for jobId: {}", event.getJobId());

        try {
            jobProducer.sendJobCreated(event);

            log.debug("Successfully sent job creation event to producer for jobId: {}", event.getJobId());

        } catch (Exception e) {
            log.error("Failed to send job creation event for jobId: {}. Error: {}", event.getJobId(), e.getMessage(), e);

            throw new BadRequestException(e.getMessage());
        }
    }


    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Async("taskExecutor")
    public void handle(JobAppliedNotificationEvent event) {
        log.info("Receive JobAppliedNotificationEvent");
        User candidate=securityUtils.getUserId(event.getCandidateId());
        User hr=securityUtils.getUserId(event.getHrId());
        sendCandidateNotification(event,candidate);
        sendHrNotification(event,candidate,hr);
    }

    private void sendCandidateNotification(JobAppliedNotificationEvent event,User candidate) {

        String title = "Ứng tuyển thành công";

        String content = """
                Bạn đã ứng tuyển thành công vào vị trí "%s" tại %s.
                Chúng tôi sẽ thông báo khi nhà tuyển dụng phản hồi hồ sơ của bạn.
                """
                .formatted(event.getNameJob(), event.getNameCompany());
        NotificationRequest request = NotificationRequest.builder()
                .user(candidate)
                .title(title)
                .content(content)
                .type(NotificationType.APPLICATION)
                .referenceId(event.getApplicationId())
                .redirectUrl("....")
                .build();

        notificationService.createNotification(request);
    }

    private void sendHrNotification(JobAppliedNotificationEvent event, User candidate, User hr) {

        String title = "Có ứng viên mới";

        String content = """
        Ứng viên **%s** vừa ứng tuyển thành công vào vị trí **"%s"**.
        """
                .formatted(candidate.getFullName(), event.getNameJob());

        NotificationRequest request = NotificationRequest.builder()
                .user(hr)
                .title(title)
                .content(content)
                .type(NotificationType.APPLICATION)
                .referenceId(event.getApplicationId())
                .redirectUrl("...")
                .build();
        notificationService.createNotification(request);

    }

}
