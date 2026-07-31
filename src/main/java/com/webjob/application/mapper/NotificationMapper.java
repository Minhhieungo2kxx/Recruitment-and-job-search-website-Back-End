package com.webjob.application.mapper;

import com.webjob.application.dto.Response.NotificationResponse;
import com.webjob.application.models.Entity.Notification;
import org.springframework.stereotype.Component;

@Component
public class NotificationMapper {
    public NotificationResponse toResponse(Notification notification) {
        if (notification == null) {
            return null;
        }

        return NotificationResponse.builder()
                .id(notification.getId())
                .title(notification.getTitle())
                .content(notification.getContent())
                .type(notification.getType())
                .referenceId(notification.getReferenceId())
                .createdAt(notification.getCreatedAt())
                .read(notification.isRead())
                .redirectUrl(notification.getRedirectUrl()==null ? null : notification.getRedirectUrl())
                .pinned(notification.isPinned())
                .build();
    }
}
