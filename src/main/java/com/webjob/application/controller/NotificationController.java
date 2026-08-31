package com.webjob.application.controller;

import com.webjob.application.annotation.RateLimit;
import com.webjob.application.dto.Response.ApiResponse;
import com.webjob.application.dto.Response.NotificationResponse;
import com.webjob.application.dto.Response.ResponseDTO;
import com.webjob.application.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {
    private final NotificationService notificationService;

//    notificationApi

    @RateLimit(maxRequests = 30, timeWindowSeconds = 60, keyType = "TOKEN")
    @GetMapping
    public ResponseEntity<ApiResponse<ResponseDTO<List<NotificationResponse>>>> getNotifications(
            @RequestParam(defaultValue = "0") int page
            , @RequestParam(defaultValue = "10") int size) {
        ApiResponse<ResponseDTO<List<NotificationResponse>>> apiResponse = new ApiResponse<>(
                HttpStatus.OK.value(),
                null,
                "Get All Notifications Successful",
                notificationService.getMyNotifications(page, size)
        );
        return ResponseEntity.ok(apiResponse);

    }


    @RateLimit(maxRequests = 60, timeWindowSeconds = 60, keyType = "TOKEN")
    @GetMapping("/unread-count")
    public ResponseEntity<ApiResponse<Long>> getUnreadCount() {
        ApiResponse<Long> apiResponse = new ApiResponse<>(
                HttpStatus.OK.value(),
                null,
                "get Unread Count Successful",
                notificationService.getUnreadCountByUser()
        );
        return ResponseEntity.ok(apiResponse);
    }

    @RateLimit(maxRequests = 10, timeWindowSeconds = 60, keyType = "TOKEN")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Object>> deleteNotification(@PathVariable Long id) {
        notificationService.deleteNotification(id);
        ApiResponse<Object> apiResponse = new ApiResponse<>(
                HttpStatus.OK.value(),
                null,
                "delete Notification Successful",
                null
        );
        return ResponseEntity.ok(apiResponse);
    }

    @DeleteMapping("/read")
    public ResponseEntity<ApiResponse<Object>> deleteAllReadNotification() {
        notificationService.deleteAllReadNotification();
        ApiResponse<Object> apiResponse = new ApiResponse<>(
                HttpStatus.OK.value(),
                null,
                "delete All Notification read True Successful",
                null
        );
        return ResponseEntity.ok(apiResponse);

    }

    @RateLimit(maxRequests = 5, timeWindowSeconds = 60, keyType = "TOKEN")
    @PatchMapping("/read-all")
    public ResponseEntity<ApiResponse<Object>> readAllNotification() {
        notificationService.markAllAsRead();
        ApiResponse<Object> apiResponse = new ApiResponse<>(
                HttpStatus.OK.value(),
                null,
                "mark All As Read Notification Successful",
                null
        );
        return ResponseEntity.ok(apiResponse);
    }

    @PatchMapping("/{id}/pin-toggle")
    public ResponseEntity<ApiResponse<Object>> togglePin(@PathVariable Long id) {

        notificationService.togglePin(id);
        return ResponseEntity.ok(
                new ApiResponse<>(
                        200,
                        null,
                        "Toggle pin successful",
                        null
                )
        );
    }

    @PatchMapping("/{id}/read-toggle")
    public ResponseEntity<?> toggleRead(@PathVariable Long id) {

        notificationService.toggleRead(id);
        return ResponseEntity.ok(
                new ApiResponse<>(
                        200,
                        null,
                        "Toggle Read successful",
                        null
                )
        );
    }
    @PatchMapping("/{id}/read")
    public ResponseEntity<Object> read(@PathVariable Long id) {
        notificationService.readById(id);
        return ResponseEntity.ok(
                new ApiResponse<>(
                        200,
                        null,
                        "Notification Read successful",
                        null
                )
        );
    }


}
//feat(notification): implement notification module with RabbitMQ and realtime support
//
//- add notification REST APIs
//        - publish JobCreatedEvent when a new job is created
//        - notify company followers through RabbitMQ
//        - process notification events with RabbitMQ consumers
//        - add Dead Letter Queue handling for failed messages
//        - support notification read, unread, pin and delete operations
//        - prepare realtime notification delivery via WebSocket for job applications, payments and application status updates
//        - improve logging and event reliability