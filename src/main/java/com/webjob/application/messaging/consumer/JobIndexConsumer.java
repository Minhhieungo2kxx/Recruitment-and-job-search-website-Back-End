package com.webjob.application.messaging.consumer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.webjob.application.document.JobDocument;
import com.webjob.application.dto.Response.RabbitEvent;
import com.webjob.application.elasticsearch.job.JobIndexService;
import com.webjob.application.enums.OutboxEventType;
import com.webjob.application.messaging.config.RabbitMQConfig;
import com.webjob.application.service.OutBox.RabbitMessageDeupService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class JobIndexConsumer {
    private final JobIndexService jobIndexService;
    private final ObjectMapper objectMapper;
    private final RabbitMessageDeupService rabbitMessageDedupService;


    @RabbitListener(
            queues = RabbitMQConfig.JOB_INDEX_QUEUE
            ,containerFactory = "rabbitListenerContainerFactory"
    )
    public void consume(RabbitEvent<String> event)  {
        String queueName = RabbitMQConfig.JOB_INDEX_QUEUE;
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

        if (OutboxEventType.JOB_INDEX_CREATED.name()
                .equals(event.getEventType())) {

            JobDocument document = parseJobDocument(event.getPayload());

            jobIndexService.indexJob(document);

        } else if (OutboxEventType.JOB_INDEX_UPDATED.name()
                .equals(event.getEventType())) {

            JobDocument document = parseJobDocument(event.getPayload());

            jobIndexService.indexJob(document);

        } else if (OutboxEventType.JOB_INDEX_DELETED.name()
                .equals(event.getEventType())) {

            JobDocument document = parseJobDocument(event.getPayload());

            jobIndexService.deleteIndexJob(document);

        } else if (OutboxEventType.JOB_INDEX_RESTORED.name()
                .equals(event.getEventType())) {

            JobDocument document = parseJobDocument(event.getPayload());

            jobIndexService.restoreIndexJob(document);

        } else if (OutboxEventType.JOB_INDEX_APPLIED_COUNT_INCREMENTED.name()
                .equals(event.getEventType())) {

            JobDocument document = parseJobDocument(event.getPayload());

            jobIndexService.incrementAppliedCount(document.getId());
        }
        else if (OutboxEventType.JOB_INDEX_APPLICATION_WITHDRAWN.name()
                .equals(event.getEventType())) {

            JobDocument document = parseJobDocument(event.getPayload());

            jobIndexService.decrementAppliedCount(document.getId());
        }
        else {
            JobDocument document = parseJobDocument(event.getPayload());
            jobIndexService.incrementViewCount(document.getId());
        }
    }



    private JobDocument parseJobDocument(String payload) {
        try {
            return objectMapper.readValue(payload, JobDocument.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }







}
