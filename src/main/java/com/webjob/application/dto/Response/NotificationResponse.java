package com.webjob.application.dto.Response;

import com.webjob.application.enums.NotificationType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class NotificationResponse {

    private Long id;

    private String title;

    private String content;

    private NotificationType type;

    private Long referenceId;

    private boolean read;

    private boolean pinned;

    private String redirectUrl;

    private Instant createdAt;

}
