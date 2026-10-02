package com.webjob.application.messaging.consumer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.webjob.application.document.CompanyDocument;
import com.webjob.application.document.JobDocument;
import com.webjob.application.dto.Response.RabbitEvent;
import com.webjob.application.elasticsearch.company.CompanyIndexService;
import com.webjob.application.enums.OutboxEventType;
import com.webjob.application.messaging.config.RabbitMQConfig;
import com.webjob.application.service.OutBox.RabbitMessageDeupService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class CommpanyIndexConsumer {
    private final CompanyIndexService companyIndexService;
    private final ObjectMapper objectMapper;
    private final RabbitMessageDeupService rabbitMessageDedupService;

    @RabbitListener(
            queues = RabbitMQConfig.COMPANY_INDEX_QUEUE,
            containerFactory = "rabbitListenerContainerFactory"
    )
    public void consume(RabbitEvent<String> event) {
        String queueName = RabbitMQConfig.COMPANY_INDEX_QUEUE;
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
            processEvent(event);
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

        } catch (Exception e) {
            log.error("Failed to process match event. eventId={}, error={}",
                    eventId, e.getMessage(), e);

            rabbitMessageDedupService.removeProcessing(
                    queueName,
                    eventId,
                    ownerToken
            );
            throw new RuntimeException(e);
        }
    }

    private void processEvent(RabbitEvent<String> event) {
        if (OutboxEventType.COMPANY_INDEX_CREATED.name().equals(event.getEventType())) {
            CompanyDocument document = parseCompanyDocument(event.getPayload());
            companyIndexService.indexCompany(document);
        } else if (OutboxEventType.COMPANY_INDEX_UPDATED.name().equals(event.getEventType())) {
            CompanyDocument document = parseCompanyDocument(event.getPayload());
            companyIndexService.indexCompany(document);

        } else if (OutboxEventType.COMPANY_INDEX_DELETED.name().equals(event.getEventType())) {
            CompanyDocument document = parseCompanyDocument(event.getPayload());
            companyIndexService.deleteIndexCompany(document.getId());

        } else {
            CompanyDocument document = parseCompanyDocument(event.getPayload());
            companyIndexService.restoreIndexCompany(document.getId());

        }

    }


    private CompanyDocument parseCompanyDocument(String payload) {
        try {
            return objectMapper.readValue(payload, CompanyDocument.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }
}
