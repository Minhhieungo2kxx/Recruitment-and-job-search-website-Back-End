package com.webjob.application.service;

import com.webjob.application.component.SecurityUtils;
import com.webjob.application.dto.Request.NotificationRequest;
import com.webjob.application.dto.Response.NotificationResponse;
import com.webjob.application.exception.Customs.BadRequestException;
import com.webjob.application.exception.Customs.ResourceNotFoundException;
import com.webjob.application.mapper.NotificationMapper;
import com.webjob.application.models.Entity.Notification;
import com.webjob.application.models.Entity.User;
import com.webjob.application.pubsub.dto.RedisMessage;
import com.webjob.application.pubsub.publisher.RedisPublisher;
import com.webjob.application.repository.NotificationRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("NotificationService Tests")
public class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private NotificationMapper notificationMapper;

    @Mock
    private SecurityUtils securityUtils;

    @Mock
    private RedisPublisher redisPublisher;

    @InjectMocks
    private NotificationService notificationService;

    private User currentUser;

    private static final Long USER_ID = 10L;
    private static final Long NOTIFICATION_ID = 100L;

    @BeforeEach
    void setUp() {
        currentUser = mock(User.class);

        lenient().when(currentUser.getId()).thenReturn(USER_ID);
        lenient().when(securityUtils.getCurrentUserId()).thenReturn(USER_ID);
    }
    // --------------------------------------------------
    // createNotification
    // --------------------------------------------------

    private NotificationRequest mockNotificationRequest() {
        NotificationRequest request = mock(NotificationRequest.class);

        when(request.getUser()).thenReturn(currentUser);
        when(request.getTitle()).thenReturn("Test title");
        when(request.getContent()).thenReturn("Test content");

        return request;
    }
    @Test
    void createNotification_shouldSaveNotificationAndPublishRedisMessage() {
        NotificationRequest request = mockNotificationRequest();

        NotificationResponse response = mock(NotificationResponse.class);

        when(notificationRepository.save(any(Notification.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        when(notificationRepository.countByUserIdAndReadFalse(USER_ID))
                .thenReturn(3L);

        when(notificationMapper.toResponse(any(Notification.class)))
                .thenReturn(response);

        notificationService.createNotification(request);

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);

        verify(notificationRepository).save(captor.capture());

        Notification saved = captor.getValue();

        assertThat(saved.getUser()).isSameAs(currentUser);
        assertThat(saved.getTitle()).isEqualTo("Test title");
        assertThat(saved.getContent()).isEqualTo("Test content");
        assertThat(saved.isRead()).isFalse();
        assertThat(saved.isPinned()).isFalse();

        verify(notificationMapper).toResponse(saved);

        verify(redisPublisher).publish(
                eq("notification"),
                any(RedisMessage.class)
        );
    }

    @Test
    void createNotification_shouldNotThrowWhenRedisPublishingFails() {
        NotificationRequest request = mockNotificationRequest();

        when(notificationRepository.save(any(Notification.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        when(notificationRepository.countByUserIdAndReadFalse(USER_ID))
                .thenReturn(0L);

        when(notificationMapper.toResponse(any(Notification.class)))
                .thenReturn(mock(NotificationResponse.class));

        doThrow(new RuntimeException("Redis unavailable"))
                .when(redisPublisher)
                .publish(eq("notification"), any(RedisMessage.class));

        assertThatCode(() ->
                notificationService.createNotification(request)
        ).doesNotThrowAnyException();

        verify(notificationRepository).save(any(Notification.class));
        verify(redisPublisher).publish(
                eq("notification"),
                any(RedisMessage.class)
        );
    }

    // --------------------------------------------------
    // getMyNotifications
    // --------------------------------------------------

    @Test
    void getMyNotifications_shouldQueryCurrentUserWithCorrectSorting() {
        Page<Notification> page = new PageImpl<>(
                List.of(mock(Notification.class)),
                PageRequest.of(0, 10),
                1
        );

        when(notificationRepository.findByUserId(eq(USER_ID), any(Pageable.class)
        )).thenReturn(page);

        when(notificationMapper.toResponse(any(Notification.class)))
                .thenReturn(mock(NotificationResponse.class));

        var result = notificationService.getMyNotifications(1, 10);

        assertThat(result).isNotNull();

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);

        verify(notificationRepository).findByUserId(eq(USER_ID), captor.capture());

        Pageable pageable = captor.getValue();

        assertThat(pageable.getPageNumber()).isZero();
        assertThat(pageable.getPageSize()).isEqualTo(10);

        List<Sort.Order> orders = pageable.getSort().stream().toList();

        assertThat(orders).hasSize(2);
        assertThat(orders.get(0).getProperty()).isEqualTo("pinned");
        assertThat(orders.get(0).getDirection())
                .isEqualTo(Sort.Direction.DESC);
        assertThat(orders.get(1).getProperty()).isEqualTo("createdAt");
        assertThat(orders.get(1).getDirection())
                .isEqualTo(Sort.Direction.DESC);
    }

    @Test
    void getMyNotifications_shouldNormalizeInvalidPageAndSize() {
        when(notificationRepository.findByUserId(
                eq(USER_ID), any(Pageable.class)
        )).thenReturn(Page.empty());

        var result = notificationService.getMyNotifications(0, 0);

        assertThat(result).isNotNull();

        ArgumentCaptor<Pageable> captor =
                ArgumentCaptor.forClass(Pageable.class);

        verify(notificationRepository)
                .findByUserId(eq(USER_ID), captor.capture());

        assertThat(captor.getValue().getPageNumber()).isZero();
        assertThat(captor.getValue().getPageSize()).isEqualTo(1);
    }

    @Test
    void getMyNotifications_shouldLimitPageSizeTo50() {
        when(notificationRepository.findByUserId(
                eq(USER_ID), any(Pageable.class)
        )).thenReturn(Page.empty());

        notificationService.getMyNotifications(2, 100);

        ArgumentCaptor<Pageable> captor =
                ArgumentCaptor.forClass(Pageable.class);

        verify(notificationRepository)
                .findByUserId(eq(USER_ID), captor.capture());

        assertThat(captor.getValue().getPageNumber()).isEqualTo(1);
        assertThat(captor.getValue().getPageSize()).isEqualTo(50);
    }

    // --------------------------------------------------
    // markAsRead / readById
    // --------------------------------------------------

    @Test
    void markAsRead_shouldMarkNotificationAsRead() {
        Notification notification = mock(Notification.class);

        when(notificationRepository.findByIdAndUserId(
                NOTIFICATION_ID, USER_ID
        )).thenReturn(Optional.of(notification));

        notificationService.markAsRead(NOTIFICATION_ID);

        verify(notification).setRead(true);
        verify(notificationRepository).save(notification);
    }

    @Test
    void markAsRead_shouldThrowWhenNotificationDoesNotExist() {
        when(notificationRepository.findByIdAndUserId(
                NOTIFICATION_ID, USER_ID
        )).thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                notificationService.markAsRead(NOTIFICATION_ID)
        ).isInstanceOf(ResourceNotFoundException.class);

        verify(notificationRepository, never())
                .save(any(Notification.class));
    }

    @Test
    void readById_shouldMarkNotificationAsReadWithoutExplicitSave() {
        Notification notification = mock(Notification.class);

        when(notificationRepository.findByIdAndUserId(
                NOTIFICATION_ID, USER_ID
        )).thenReturn(Optional.of(notification));

        notificationService.readById(NOTIFICATION_ID);

        verify(notification).setRead(true);
        verify(notificationRepository, never())
                .save(any(Notification.class));
    }

    @Test
    void readById_shouldThrowWhenNotificationDoesNotExist() {
        when(notificationRepository.findByIdAndUserId(
                NOTIFICATION_ID, USER_ID
        )).thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                notificationService.readById(NOTIFICATION_ID)
        ).isInstanceOf(BadRequestException.class);
    }

    // --------------------------------------------------
    // Unread count
    // --------------------------------------------------

    @Test
    void getUnreadCount_shouldReturnRepositoryCount() {
        when(notificationRepository.countByUserIdAndReadFalse(USER_ID))
                .thenReturn(5L);

        long result = notificationService.getUnreadCount(USER_ID);

        assertThat(result).isEqualTo(5L);

        verify(notificationRepository)
                .countByUserIdAndReadFalse(USER_ID);
    }

    @Test
    void getUnreadCountByUser_shouldUseCurrentUserId() {
        when(notificationRepository.countByUserIdAndReadFalse(USER_ID))
                .thenReturn(2L);

        long result = notificationService.getUnreadCountByUser();

        assertThat(result).isEqualTo(2L);

        verify(notificationRepository)
                .countByUserIdAndReadFalse(USER_ID);
    }

    // --------------------------------------------------
    // Delete notifications
    // --------------------------------------------------

    @Test
    void deleteNotification_shouldDeleteNotificationOwnedByCurrentUser() {
        Notification notification = mock(Notification.class);

        when(notificationRepository.findByIdAndUserId(
                NOTIFICATION_ID, USER_ID
        )).thenReturn(Optional.of(notification));

        notificationService.deleteNotification(NOTIFICATION_ID);

        verify(notificationRepository)
                .findByIdAndUserId(NOTIFICATION_ID, USER_ID);

        verify(notificationRepository).delete(notification);
    }

    @Test
    void deleteNotification_shouldThrowWhenNotificationDoesNotExist() {
        when(notificationRepository.findByIdAndUserId(
                NOTIFICATION_ID, USER_ID
        )).thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                notificationService.deleteNotification(NOTIFICATION_ID)
        ).isInstanceOf(RuntimeException.class)
                .hasMessage("Notification not found");

        verify(notificationRepository, never())
                .delete(any(Notification.class));
    }

    @Test
    void deleteAllReadNotification_shouldDeleteOnlyCurrentUserReadNotifications() {
        notificationService.deleteAllReadNotification();

        verify(notificationRepository)
                .deleteByUserIdAndReadTrue(USER_ID);
    }

    // --------------------------------------------------
    // markAllAsRead
    // --------------------------------------------------

    @Test
    void markAllAsRead_shouldMarkAllUnreadNotificationsAsRead() {
        Notification first = mock(Notification.class);
        Notification second = mock(Notification.class);

        when(notificationRepository.findByUserIdAndReadFalse(USER_ID))
                .thenReturn(List.of(first, second));

        notificationService.markAllAsRead();

        verify(first).setRead(true);
        verify(second).setRead(true);

        verify(notificationRepository).saveAll(List.of(first, second));
    }

    @Test
    void markAllAsRead_shouldHandleEmptyList() {
        when(notificationRepository.findByUserIdAndReadFalse(USER_ID))
                .thenReturn(List.of());

        notificationService.markAllAsRead();

        verify(notificationRepository).saveAll(List.of());
    }

    // --------------------------------------------------
    // Pin
    // --------------------------------------------------

    @Test
    void togglePin_shouldPinNotificationWhenCurrentlyUnpinned() {
        Notification notification = mock(Notification.class);

        when(notificationRepository.findByIdAndUserId(
                NOTIFICATION_ID, USER_ID
        )).thenReturn(Optional.of(notification));

        when(notification.isPinned()).thenReturn(false);

        notificationService.togglePin(NOTIFICATION_ID);

        verify(notification).setPinned(true);
        verify(notificationRepository).save(notification);
    }

    @Test
    void togglePin_shouldUnpinNotificationWhenCurrentlyPinned() {
        Notification notification = mock(Notification.class);

        when(notificationRepository.findByIdAndUserId(
                NOTIFICATION_ID, USER_ID
        )).thenReturn(Optional.of(notification));

        when(notification.isPinned()).thenReturn(true);

        notificationService.togglePin(NOTIFICATION_ID);

        verify(notification).setPinned(false);
        verify(notificationRepository).save(notification);
    }

    @Test
    void togglePin_shouldThrowWhenNotificationDoesNotExist() {
        when(notificationRepository.findByIdAndUserId(
                NOTIFICATION_ID, USER_ID
        )).thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                notificationService.togglePin(NOTIFICATION_ID)
        ).isInstanceOf(BadRequestException.class);

        verify(notificationRepository, never())
                .save(any(Notification.class));
    }

    // --------------------------------------------------
    // Toggle read
    // --------------------------------------------------

    @Test
    void toggleRead_shouldInvertReadStatus() {
        Notification notification = mock(Notification.class);

        when(notificationRepository.findByIdAndUserId(
                NOTIFICATION_ID, USER_ID
        )).thenReturn(Optional.of(notification));

        when(notification.isRead()).thenReturn(false);

        notificationService.toggleRead(NOTIFICATION_ID);

        verify(notification).setRead(true);
    }

    @Test
    void toggleRead_shouldMarkUnreadWhenCurrentlyRead() {
        Notification notification = mock(Notification.class);

        when(notificationRepository.findByIdAndUserId(
                NOTIFICATION_ID, USER_ID
        )).thenReturn(Optional.of(notification));

        when(notification.isRead()).thenReturn(true);

        notificationService.toggleRead(NOTIFICATION_ID);

        verify(notification).setRead(false);
    }

    @Test
    void toggleRead_shouldThrowWhenNotificationDoesNotExist() {
        when(notificationRepository.findByIdAndUserId(
                NOTIFICATION_ID, USER_ID
        )).thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                notificationService.toggleRead(NOTIFICATION_ID)
        ).isInstanceOf(BadRequestException.class);
    }

    // --------------------------------------------------
    // Test data
    // --------------------------------------------------




}
