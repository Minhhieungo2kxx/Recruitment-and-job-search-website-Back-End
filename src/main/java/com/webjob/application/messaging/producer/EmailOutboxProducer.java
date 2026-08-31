package com.webjob.application.messaging.producer;

import com.webjob.application.dto.Response.RabbitEvent;
import com.webjob.application.models.Entity.OutboxEvent;
import com.webjob.application.service.OutBox.OutboxService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class EmailOutboxProducer {
    private final OutboxService outboxService;
    private final RabbitTemplate rabbitTemplate;

    @Scheduled(fixedDelay =  10000)
    public void publishEvents() {

        List<OutboxEvent> events = outboxService.claimEmailEvents();
        if (events.isEmpty()) {
            return;
        }
        log.info("Starting EmailOutbox scheduled batch publication of {} outbox events", events.size());
        for (OutboxEvent event : events) {
            publish(event);
        }
        log.info("Finished EmailOutbox scheduled batch publication cycle");
    }

    private void publish(OutboxEvent event) {

        try {
            RabbitEvent<String> message = RabbitEvent.<String>builder()
                    .eventId(event.getEventId())
                    .eventType(event.getEventType().name())
                    .aggregateType(event.getAggregateType())
                    .aggregateId(event.getAggregateId())
                    .payload(event.getPayload())
                    .build();


            rabbitTemplate.convertAndSend(
                    event.getExchangeName(),
                    event.getRoutingKey(),
                    message
            );
            outboxService.markPublished(event.getId());
            log.debug("Successfully published EmailOutbox outbox event - ID: {}, Exchange: {}, RoutingKey: {}",
                    event.getId(), event.getExchangeName(), event.getRoutingKey());


        } catch (Exception e) {

            log.error("Failed EmailOutbox to publish outbox event id={}", event.getId(), e);

            outboxService.handleFailure(event.getId(), e.getMessage());
        }
    }
}
