package com.webjob.application.models.Entity;

import com.webjob.application.enums.OutboxCategory;
import com.webjob.application.enums.OutboxEventType;
import com.webjob.application.enums.OutboxStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "outbox_events",

        indexes = {
                @Index(
                        name = "idx_outbox_category_status_retry",
                        columnList = "category, status, next_retry_at, id"
                ),

                @Index(
                        name = "idx_outbox_processing_lock",
                        columnList = "status, locked_until"
                )
        }
)

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OutboxEvent {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

//    Unique ID của event.
    @Column(name = "event_id", nullable = false, unique = true, updatable = false)
    private String eventId;

//     Loại entity phát sinh event.
    @Column(name = "aggregate_type", nullable = false, length = 50)
    private String aggregateType;


//    ID của entity phát sinh event.
    @Column(name = "aggregate_id", nullable = false)
    private String aggregateId;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false, length = 30)
    private OutboxCategory category;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 50)
    private OutboxEventType eventType;

    @Column(name = "exchange_name", nullable = false, length = 100)
    private String exchangeName;

    /**
     * RabbitMQ routing key.
     */
    @Column(name = "routing_key", nullable = false, length = 150)
    private String routingKey;

//    JSON payload gửi sang RabbitMQ.
    @Column(name = "payload", nullable = false, columnDefinition = "TEXT")
    private String payload;

//    Trạng thái publish event.
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private OutboxStatus status = OutboxStatus.PENDING;


//     Thời điểm event được tạo.
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

//     Thời điểm publish thành công.
    @Column(name = "published_at")
    private Instant publishedAt;

//     Lỗi gần nhất nếu publish thất bại.
    @Column(name = "last_error", columnDefinition = "TEXT")
    private String lastError;



    //recover PROCESSING nếu worker chết
    @Column(name = "locked_until")
    private Instant lockedUntil;

//    Thời điểm retry tiếp theo.
    @Column(name = "next_retry_at")
    private Instant nextRetryAt;

//    * Số lần đã thử publish.
    @Column(name = "retry_count", nullable = false)
    @Builder.Default
    private int retryCount = 0;

    @PrePersist
    protected void onCreate() {
        if (eventId == null) {
            eventId = UUID.randomUUID().toString();
        }

        if (createdAt == null) {
            createdAt = Instant.now();
        }

        if (status == null) {
            status = OutboxStatus.PENDING;
        }

        if (nextRetryAt == null) {
            nextRetryAt = createdAt;
        }
    }
}

