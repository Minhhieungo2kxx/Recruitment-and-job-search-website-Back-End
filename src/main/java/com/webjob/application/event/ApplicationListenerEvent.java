package com.webjob.application.event;

import com.webjob.application.dto.Request.NotificationRequest;
import com.webjob.application.enums.NotificationType;
import com.webjob.application.enums.ResumeStatus;
import com.webjob.application.event.dto.ApplicationStatusChangedEvent;
import com.webjob.application.models.Entity.User;
import com.webjob.application.service.NotificationService;
import com.webjob.application.utils.common.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class ApplicationListenerEvent {
    private final NotificationService notificationService;
    private final SecurityUtils securityUtils;


    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Async("taskExecutor")
    public void handleApplicationStatusChanged(ApplicationStatusChangedEvent event) {
        User candidate = securityUtils.getUserId(event.getCandidateId());

        NotificationRequest request = NotificationRequest.builder()
                .user(candidate)
                .title(buildTitle(event.getNewStatus()))
                .content(buildContent(event))
                .type(NotificationType.APPLICATION)
                .referenceId(event.getApplicationId())
                .redirectUrl("/candidate/applications/" + event.getApplicationId())
                .build();

        notificationService.createNotification(request);

    }

    private String buildTitle(ResumeStatus status){
        return switch(status){
            case REVIEWING ->
                    "👀 Hồ sơ đang được xem xét";
            case INTERVIEWING ->
                    "🎉 Bạn nhận được lịch phỏng vấn!";
            case OFFERED ->
                    "💼 Bạn đã nhận được lời mời làm việc (Offer)";
            case HIRED ->
                    "🚀 Chúc mừng bạn chính thức gia nhập công ty!";
            case REJECTED ->
                    "📌 Cập nhật kết quả ứng tuyển";
            default ->
                    "🔔 Cập nhật trạng thái hồ sơ";
        };
    }
    private String buildContent(ApplicationStatusChangedEvent event){
        return switch(event.getNewStatus()){
            case REVIEWING ->
                    """
                    Nhà tuyển dụng tại %s đang xem xét hồ sơ ứng tuyển của bạn cho vị trí "%s".
                    Hãy theo dõi sát sao các thông báo tiếp theo nhé!
                    """
                            .formatted(event.getCompanyName(), event.getJobName());

            case INTERVIEWING ->
                    """
                    Chúc mừng bạn! Hồ sơ xuất sắc của bạn đã chinh phục nhà tuyển dụng và bạn chính thức nhận được lời mời phỏng vấn cho vị trí "%s" tại %s. 
                    Hãy chuẩn bị tinh thần thật tốt và tự tin tỏa sáng nhé! Mọi thông tin chi tiết về lịch trình và hướng dẫn phỏng vấn đã được gửi trọn vẹn vào email của bạn.
                    """
                            .formatted(event.getJobName(), event.getCompanyName());

            case OFFERED ->
                    """
                    Tuyệt vời! %s đã gửi lời mời làm việc (Offer) cho vị trí "%s".
                    Bạn vui lòng phản hồi lại email của công ty để xác nhận nhận việc hoặc trao đổi thêm nhé.
                    """
                            .formatted(event.getCompanyName(), event.getJobName());

            case HIRED ->
                    """
                    Chúc mừng bạn đã vượt qua tất cả các vòng! Bạn chính thức trở thành nhân viên mới của %s.
                    Chúc bạn có một hành trình làm việc thật nhiều thành công và truyền cảm hứng!
                    """
                            .formatted(event.getCompanyName());

            case REJECTED ->
                    """
                    Cảm ơn bạn đã quan tâm và dành thời gian ứng tuyển vào vị trí "%s" tại %s. 
                    Rất tiếc hiện tại hồ sơ của bạn chưa phù hợp với tiêu chí tuyển dụng. Chúc bạn sẽ sớm tìm được cơ hội việc làm phù hợp hơn trong thời gian tới.
                    """
                            .formatted(
                                    event.getJobName(),
                                    event.getCompanyName()
                            );

            default ->
                    "Trạng thái hồ sơ của bạn đã được cập nhật. Vui lòng kiểm tra chi tiết trên hệ thống.";
        };
    }
}
