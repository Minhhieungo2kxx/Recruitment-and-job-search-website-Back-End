package com.webjob.application.repository;

import com.webjob.application.enums.OutboxStatus;
import com.webjob.application.models.Entity.OutboxEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface OutboxEventRepository extends JpaRepository<OutboxEvent, Long> {

    @Query(value = """
            SELECT *
            FROM outbox_events
            WHERE category = :category
              AND status = 'PENDING'
              AND next_retry_at <= :now
            ORDER BY id
            LIMIT :limit
            FOR UPDATE SKIP LOCKED
            """, nativeQuery = true)
    List<OutboxEvent> findEventsForProcessing(
            @Param("category") String category,
            @Param("now") Instant now,
            @Param("limit") int limit
    );

    @Modifying
    @Query("""
            UPDATE OutboxEvent e
               SET e.status = :pending,
                   e.lockedUntil = null,
                   e.nextRetryAt = :now
             WHERE e.status = :processing
               AND e.lockedUntil <= :now
            """)
    int recoverExpiredProcessingEvents(
            @Param("processing") OutboxStatus processing,
            @Param("pending") OutboxStatus pending,
            @Param("now") Instant now
    );
}
