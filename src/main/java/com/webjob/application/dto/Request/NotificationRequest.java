package com.webjob.application.dto.Request;

import com.webjob.application.enums.NotificationType;
import com.webjob.application.models.Entity.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class NotificationRequest {
    private User user;
    private String title;
    private String content;
    private NotificationType type;
    private Long referenceId;
    private String redirectUrl;
}
