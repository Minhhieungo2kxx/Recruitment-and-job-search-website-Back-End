package com.webjob.application.service.OutBox;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.webjob.application.dto.Request.OutboxDTO;
import com.webjob.application.enums.OutboxCategory;
import com.webjob.application.enums.OutboxStatus;
import com.webjob.application.exception.Customs.ResourceNotFoundException;
import com.webjob.application.models.Entity.OutboxEvent;
import com.webjob.application.repository.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class OutboxService {
    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;
    private static final int MAX_RETRY_COUNT = 10;


    public void save(OutboxDTO outboxDTO) {
        try {
            String json = objectMapper.writeValueAsString(outboxDTO.getPayload());

            OutboxEvent event = OutboxEvent.builder()
                    .eventId(UUID.randomUUID().toString())
                    .aggregateType(outboxDTO.getAggregateType())
                    .aggregateId(outboxDTO.getAggregateId())
                    .eventType(outboxDTO.getEventType())
                    .category(outboxDTO.getCategory())
                    .exchangeName(outboxDTO.getExchangeName())
                    .routingKey(outboxDTO.getRoutingKey())
                    .payload(json)
                    .status(OutboxStatus.PENDING)
                    .createdAt(Instant.now())
                    .nextRetryAt(Instant.now())
                    .retryCount(0)
                    .build();
            outboxEventRepository.save(event);
            log.debug("Successfully saved outbox event with ID: {} for aggregate: {}",
                    event.getEventId(), event.getAggregateId());

        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Cannot serialize outbox event payload", e);
        }
    }

    @Transactional
    public List<OutboxEvent> claimJobIndexEvents() {

        Instant now = Instant.now();
        Instant lockedUntil = now.plusSeconds(60);

        List<OutboxEvent> events = outboxEventRepository.findEventsForProcessing(
                OutboxCategory.JOB_INDEX.name(),
                now,
                100
        );
        if (events.isEmpty()) {
            log.debug("No pending job index events found to claim at time: {}", now);
            return events;
        }

        for (OutboxEvent event : events) {
            event.setStatus(OutboxStatus.PROCESSING);
            event.setLockedUntil(lockedUntil);
        }
        log.info("Claimed {} job index events for processing, locked until: {}", events.size(), lockedUntil);

        return events;
    }

    @Transactional
    public List<OutboxEvent> claimCompanyIndexEvents() {

        Instant now = Instant.now();
        Instant lockedUntil = now.plusSeconds(60);

        List<OutboxEvent> events = outboxEventRepository.findEventsForProcessing(
                OutboxCategory.COMPANY_INDEX.name(),
                now,
                100
        );
        if (events.isEmpty()) {
            log.debug("No pending company index events found to claim at time: {}", now);
            return events;
        }

        for (OutboxEvent event : events) {
            event.setStatus(OutboxStatus.PROCESSING);
            event.setLockedUntil(lockedUntil);
        }
        log.info("Claimed {} company index events for processing, locked until: {}", events.size(), lockedUntil);
        return events;
    }

    @Transactional
    public List<OutboxEvent> claimEmailEvents() {

        Instant now = Instant.now();
        Instant lockedUntil = now.plusSeconds(60);

        List<OutboxEvent> events = outboxEventRepository.findEventsForProcessing(
                OutboxCategory.EMAIL.name(),
                now,
                100
        );
        if (events.isEmpty()) {
            log.debug("No pending email events found to claim at time: {}", now);
            return events;
        }

        for (OutboxEvent event : events) {
            event.setStatus(OutboxStatus.PROCESSING);
            event.setLockedUntil(lockedUntil);
        }

        log.info("Claimed {} email events for processing, locked until: {}", events.size(), lockedUntil);
        return events;
    }

    @Transactional
    public void markPublished(Long Id) {
        OutboxEvent event = outboxEventRepository.findById(Id)
                .orElseThrow(() -> {
                    log.warn("Attempted to mark non-existent outbox event as published. ID: {}", Id);
                    return new ResourceNotFoundException("Outbox event not found: " + Id);
                });

        event.setStatus(OutboxStatus.PUBLISHED);
        event.setPublishedAt(Instant.now());
        event.setLockedUntil(null);
        event.setLastError(null);
        outboxEventRepository.save(event);
        log.debug("Successfully marked outbox event as published. ID: {}, AggregateId: {}",
                event.getEventId(), event.getAggregateId());
    }

    @Transactional
    public void handleFailure(Long Id, String error) {
        OutboxEvent event = outboxEventRepository.findById(Id)
                .orElseThrow(() -> {
                    log.warn("Attempted to handle failure for non-existent outbox event. ID: {}", Id);
                    return new ResourceNotFoundException("Outbox event not found: " + Id);
                });

        int retryCount = event.getRetryCount() + 1;
        event.setRetryCount(retryCount);

        boolean isFailed = retryCount >= MAX_RETRY_COUNT;
        event.setStatus(isFailed ? OutboxStatus.FAILED : OutboxStatus.PENDING);
        event.setLastError(error);
        event.setLockedUntil(null);

        if (!isFailed) {
            long delaySeconds = Math.min(300, (long) Math.pow(2, retryCount));
            event.setNextRetryAt(Instant.now().plusSeconds(delaySeconds));
            log.warn("Outbox event failed, scheduled for retry. ID: {}, RetryCount: {}, NextRetryIn: {}s, Error: {}",
                    Id, retryCount, delaySeconds, error);
        } else {
            log.error("Outbox event reached max retry limit and marked as FAILED. ID: {}, MaxRetries: {}, Error: {}",
                    Id, MAX_RETRY_COUNT, error);
        }

        outboxEventRepository.save(event);
    }


    @Transactional
    public int recoverExpiredEvents() {

        return outboxEventRepository.recoverExpiredProcessingEvents(
                OutboxStatus.PROCESSING,
                OutboxStatus.PENDING,
                Instant.now()
        );
    }
}


