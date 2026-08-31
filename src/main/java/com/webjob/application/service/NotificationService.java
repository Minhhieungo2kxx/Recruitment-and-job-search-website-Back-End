package com.webjob.application.service;


import com.webjob.application.dto.Request.NotificationRequest;
import com.webjob.application.dto.Response.*;
import com.webjob.application.enums.NotificationType;
import com.webjob.application.exception.Customs.BadRequestException;
import com.webjob.application.exception.Customs.ResourceNotFoundException;
import com.webjob.application.exception.Customs.UnauthorizedException;
import com.webjob.application.mapper.NotificationMapper;
import com.webjob.application.models.Entity.JobAlert;
import com.webjob.application.models.Entity.Notification;
import com.webjob.application.models.Entity.User;
import com.webjob.application.pubsub.dto.RedisMessage;
import com.webjob.application.pubsub.publisher.RedisPublisher;
import com.webjob.application.repository.NotificationRepository;
import com.webjob.application.repository.UserRepository;
import com.webjob.application.utils.common.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class NotificationService {
    private final NotificationRepository notificationRepository;

    private final SimpMessagingTemplate messagingTemplate;

    private final NotificationMapper notificationMapper;
    private final SecurityUtils securityUtils;
    private final RedisPublisher redisPublisher;


    @Transactional
    public void createNotification(NotificationRequest request) {
        log.info("createNotification called");

        Notification notification = Notification.builder()
                .user(request.getUser()).title(request.getTitle())
                .content(request.getContent()).type(request.getType())
                .referenceId(request.getReferenceId())
                .redirectUrl(request.getRedirectUrl())
                .read(false).pinned(false)
                .build();
        Notification saved = notificationRepository.save(notification);
        log.info("notification id={}", saved.getId());

        NotificationWS notificationWS = NotificationWS.builder()
                .notification(notificationMapper.toResponse(saved))
                .unreadCount(getUnreadCount(saved.getUser().getId()))
                .build();
        try {
            redisPublisher.publish("notification", RedisMessage.builder()
                    .userId(String.valueOf(request.getUser().getId()))
                    .destination("/queue/notification")
                    .payload(notificationWS)
                    .build());
        } catch (Exception e) {
            log.warn("Failed to send websocket notification", e);
        }

    }

    @Transactional(readOnly = true)
    public ResponseDTO<List<NotificationResponse>> getMyNotifications(int page, int size) {

        size = Math.min(Math.max(size, 1), 50);
        page = Math.max(page, 1);

        Pageable pageable = PageRequest.of(page - 1, size,
                Sort.by(Sort.Direction.DESC, "pinned")
                        .and(Sort.by(Sort.Direction.DESC, "createdAt")));
        Long userId = securityUtils.getCurrentUserId();
        Page<Notification> pagelist = notificationRepository.findByUserId(userId, pageable);

        int currentpage = pagelist.getNumber() + 1;
        int pagesize = pagelist.getSize();
        int totalpage = pagelist.getTotalPages();
        Long totalItem = pagelist.getTotalElements();

        MetaDTO metaDTO = new MetaDTO(currentpage, pagesize, totalpage, totalItem);
        List<NotificationResponse> list = pagelist.getContent().stream()
                .map(notificationMapper::toResponse)
                .toList();

        return new ResponseDTO<>(metaDTO, list);

    }

    @Transactional
    public void markAsRead(Long notificationId) {

        Notification notification = notificationRepository
                .findByIdAndUserId(notificationId, securityUtils.getCurrentUserId())
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found"));

        notification.setRead(true);
        notificationRepository.save(notification);
    }

    @Transactional(readOnly = true)
    public long getUnreadCount(Long userId) {
        return notificationRepository.countByUserIdAndReadFalse(userId);
    }

    @Transactional(readOnly = true)
    public long getUnreadCountByUser() {

        return notificationRepository.countByUserIdAndReadFalse(securityUtils.getCurrentUserId());
    }

    @Transactional
    public void deleteNotification(Long notificationId) {

        User user = securityUtils.getCurrentUser();
        Notification notification = notificationRepository
                .findByIdAndUserId(notificationId, securityUtils.getCurrentUserId())
                .orElseThrow(() -> new RuntimeException("Notification not found"));
        notificationRepository.delete(notification);
    }

    @Transactional
    public void deleteAllReadNotification() {
        notificationRepository.deleteByUserIdAndReadTrue(securityUtils.getCurrentUserId());
    }


    @Transactional
    public void markAllAsRead() {
        Long userId = securityUtils.getCurrentUserId();

        List<Notification> notifications = notificationRepository.findByUserIdAndReadFalse(userId);
        notifications.forEach(notification -> notification.setRead(true));
        notificationRepository.saveAll(notifications);
    }


    @Transactional
    public void togglePin(Long id) {
        Notification notification = notificationRepository
                .findByIdAndUserId(id, securityUtils.getCurrentUserId())
                .orElseThrow(() -> new BadRequestException("Notification not found"));
        notification.setPinned(!notification.isPinned());
        notificationRepository.save(notification);
    }

    @Transactional
    public void toggleRead(Long id) {

        Notification notification = notificationRepository
                .findByIdAndUserId(id, securityUtils.getCurrentUserId())
                .orElseThrow(() -> new BadRequestException("Notification not found"));

        notification.setRead(!notification.isRead());

    }
    @Transactional
    public void readById(Long id) {
        Notification notification = notificationRepository
                .findByIdAndUserId(id, securityUtils.getCurrentUserId())
                .orElseThrow(() -> new BadRequestException("Notification not found"));
        notification.setRead(true);
    }

}
