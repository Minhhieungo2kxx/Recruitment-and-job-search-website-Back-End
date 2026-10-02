package com.webjob.application.messaging.consumer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.webjob.application.document.CompanyDocument;
import com.webjob.application.dto.Response.RabbitEvent;
import com.webjob.application.elasticsearch.company.CompanyIndexService;
import com.webjob.application.enums.OutboxEventType;
import com.webjob.application.messaging.config.RabbitMQConfig;
import com.webjob.application.service.JobMatchingHR.Matching.CvJobMatchingService;
import com.webjob.application.service.OutBox.RabbitMessageDeupService;
import com.webjob.application.utils.common.UtilFormat;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class CandidateMatchConsumer {

    private final CvJobMatchingService cvJobMatchingService;
    private final RabbitMessageDeupService rabbitMessageDedupService;

    @RabbitListener(
            queues = RabbitMQConfig.CANDIDATE_MATCH_QUEUE,
            containerFactory = "rabbitListenerContainerFactory"
    )
    public void consume(RabbitEvent<String> event) {
        String queueName = RabbitMQConfig.CANDIDATE_MATCH_QUEUE;
        String eventId = event.getEventId();
        String ownerToken = rabbitMessageDedupService.tryStartProcessing(queueName, eventId);

        if (ownerToken == null) {
            log.info("Duplicate/in-flight event ignored. eventId={}, eventType={}",
                    eventId,
                    event.getEventType()
            );
            return;
        }

        try {
            cvJobMatchingService.runMatchingForApplication(UtilFormat.asLong(event.getPayload()));
            boolean markedProcessed = rabbitMessageDedupService.markProcessed(
                            queueName,
                            eventId,
                            ownerToken
                    );
            if (!markedProcessed) {
                /*
                 * Ownership đã bị mất.
                 *
                 * Không nên im lặng coi đây là success.
                 * Tùy business có thể throw exception để RabbitMQ retry.
                 */
                throw new IllegalStateException(
                        "Lost ownership while marking event as processed. " +
                                "eventId=" + eventId
                );
            }
            log.info("Successfully processed candidate match event. eventId={}", eventId);

        } catch (Exception e) {

            log.error("Failed to process candidate match event. eventId={}, error={}",
                    eventId, e.getMessage(), e);

            rabbitMessageDedupService.removeProcessing(
                    queueName,
                    eventId,
                    ownerToken
            );
            throw new RuntimeException(e);
        }
    }



}


