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
        boolean acquired = rabbitMessageDedupService.tryStartProcessing(queueName, eventId
        );
        if (!acquired) {
            log.info("Duplicate/in-flight event ignored. eventId={}, eventType={}",
                    eventId, event.getEventType());
            return;
        }
        try {
            processEvent(event);
            rabbitMessageDedupService.markProcessed(queueName, eventId);

        } catch (Exception e) {
            rabbitMessageDedupService.removeProcessing(queueName, eventId);
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
