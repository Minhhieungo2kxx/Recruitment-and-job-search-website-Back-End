package com.webjob.application.messaging.producer;

import com.webjob.application.dto.Response.RabbitEvent;
import com.webjob.application.messaging.config.RabbitMQConfig;
import com.webjob.application.models.Entity.OutboxEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class CandidateMatchProducer {
    private final RabbitTemplate rabbitTemplate;

    private static final String EVENT_TYPE = "CANDIDATE_MATCH";
    private static final String AGGREGATE_TYPE = "APPLICATION";

    @Retryable(
            retryFor = Exception.class,
            maxAttempts = 3,
            backoff = @Backoff(delay = 1000, multiplier = 2)
    )
    public void publishEvent(Long applicationId) {
        if (applicationId == null) {
            throw new IllegalArgumentException("applicationId must not be null");
        }
        String applicationIdValue = applicationId.toString();

        try {
            RabbitEvent<String> message = RabbitEvent.<String>builder()
                    .eventId(applicationIdValue)
                    .eventType(EVENT_TYPE)
                    .aggregateType(AGGREGATE_TYPE)
                    .aggregateId(applicationIdValue)
                    .payload(applicationIdValue)
                    .build();

            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.CANDIDATE_MATCH_EXCHANGE,
                    RabbitMQConfig.CANDIDATE_MATCH_ROUTING_KEY,
                    message
            );
            log.debug(
                    "Successfully published CandidateMatch event - applicationId={}, exchange={}, routingKey={}",
                    applicationId,
                    RabbitMQConfig.CANDIDATE_MATCH_EXCHANGE,
                    RabbitMQConfig.CANDIDATE_MATCH_ROUTING_KEY
            );

        } catch (Exception e) {
            log.error("Failed to publish CandidateMatch event - applicationId={}", applicationId, e);
            throw e;
        }
    }


}






