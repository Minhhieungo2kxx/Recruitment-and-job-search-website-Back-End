package com.webjob.application.messaging.config;

import com.webjob.application.dto.record.RabbitQueueConfig;
import org.springframework.amqp.core.*;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

@Configuration
public class RabbitMQConfig {

    // TopicExchange main chung
    public static final String EMAIL_EXCHANGE = "email.exchange";

    //    cron subscriberId job with skill
    public static final String EMAIL_QUEUE = "email.queue";
    public static final String EMAIL_ROUTING_KEY = "email.job";
    public static final String DLX_EXCHANGE = "email.dlx";
    public static final String DLQ_QUEUE = "email.dead.queue";
    public static final String DLQ_ROUTING_KEY = "email.dead";

    // Forgot password
    public static final String FORGOT_QUEUE = "forgot-password.queue";
    public static final String FORGOT_ROUTING_KEY = "email.forgot";
    public static final String FORGOT_DLX = "forgot-password.dlx";
    public static final String FORGOT_DLQ = "forgot-password.dead.queue";
    public static final String FORGOT_DLQ_ROUTING = "email.forgot.dead";

    //    JOB_APPLY
    public static final String JOB_APPLY_QUEUE = "job.apply.queue";
    public static final String JOB_APPLY_ROUTING_KEY = "job.apply";
    public static final String JOB_APPLY_DLX = "job.apply.dlx";
    public static final String JOB_APPLY_DLQ = "job.apply.dead.queue";
    public static final String JOB_APPLY_DLQ_ROUTING = "job.apply.dead";

    //    job alert
    public static final String JOB_ALERT_QUEUE = "job.alert.queue";
    public static final String JOB_ALERT_ROUTING_KEY = "job.alert";
    public static final String JOB_ALERT_DLX = "job.alert.dlx";
    public static final String JOB_ALERT_DLQ = "job.alert.dead.queue";
    public static final String JOB_ALERT_DLQ_ROUTING = "job.alert.dead";

    // --- Cấu hình cho Follow Company Job Notification ---
    public static final String FOLLOW_COMPANY_EXCHANGE = "follow.company.exchange";
    public static final String FOLLOW_COMPANY_JOB_QUEUE = "follow.company.job.queue";
    public static final String FOLLOW_COMPANY_JOB_ROUTING_KEY = "follow.company.job.#";
    public static final String FOLLOW_COMPANY_JOB_DLX = "follow.company.job.dlx";
    public static final String FOLLOW_COMPANY_JOB_DLQ = "follow.company.job.dead.queue";
    public static final String FOLLOW_COMPANY_JOB_DLQ_ROUTING = "follow.company.job.dead";

// JOB INDEX
    public static final String JOB_INDEX_EXCHANGE = "job.index.exchange";
    public static final String JOB_INDEX_QUEUE = "job.index.queue";
    public static final String JOB_INDEX_CREATED_ROUTING_KEY = "job.index.created";
    public static final String JOB_INDEX_UPDATED_ROUTING_KEY = "job.index.updated";
    public static final String JOB_INDEX_DELETED_ROUTING_KEY = "job.index.deleted";
    public static final String JOB_INDEX_RESTORED_ROUTING_KEY = "job.index.restored";
    public static final String JOB_INDEX_APPLIED_COUNT_INCREMENTED_ROUTING_KEY =
            "job.index.applied-count.incremented";
    public static final String JOB_INDEX_APPLICATION_WITHDRAWN_ROUTING_KEY =
            "job.index.application.withdrawn";
    public static final String JOB_INDEX_VIEW_COUNT_INCREMENTED_ROUTING_KEY =
            "job.index.view-count.incremented";

    public static final String JOB_INDEX_DLX = "job.index.dlx";

    public static final String JOB_INDEX_DLQ = "job.index.dead.queue";

    public static final String JOB_INDEX_DLQ_ROUTING_KEY = "job.index.dead";

    // COMPANY INDEX
    public static final String COMPANY_INDEX_EXCHANGE = "company.index.exchange";

    public static final String COMPANY_INDEX_QUEUE = "company.index.queue";

    public static final String COMPANY_INDEX_CREATED_ROUTING_KEY = "company.index.created";

    public static final String COMPANY_INDEX_UPDATED_ROUTING_KEY = "company.index.updated";

    public static final String COMPANY_INDEX_DELETED_ROUTING_KEY = "company.index.deleted";

    public static final String COMPANY_INDEX_RESTORED_ROUTING_KEY = "company.index.restored";


    public static final String COMPANY_INDEX_DLX = "company.index.dlx";

    public static final String COMPANY_INDEX_DLQ = "company.index.dead.queue";

    public static final String COMPANY_INDEX_DLQ_ROUTING_KEY = "company.index.dead";


    @Bean
    public Declarables rabbitDeclarables() {

        List<RabbitQueueConfig> configs = List.of(

                // Email
                new RabbitQueueConfig(
                        EMAIL_EXCHANGE,
                        EMAIL_QUEUE,
                        List.of(EMAIL_ROUTING_KEY),
                        DLX_EXCHANGE,
                        DLQ_QUEUE,
                        DLQ_ROUTING_KEY
                ),

                // Forgot password
                new RabbitQueueConfig(
                        EMAIL_EXCHANGE,
                        FORGOT_QUEUE,
                        List.of(FORGOT_ROUTING_KEY),
                        FORGOT_DLX,
                        FORGOT_DLQ,
                        FORGOT_DLQ_ROUTING
                ),

                // Job Apply
                new RabbitQueueConfig(
                        EMAIL_EXCHANGE,
                        JOB_APPLY_QUEUE,
                        List.of(JOB_APPLY_ROUTING_KEY),
                        JOB_APPLY_DLX,
                        JOB_APPLY_DLQ,
                        JOB_APPLY_DLQ_ROUTING
                ),

                // Job Alert
                new RabbitQueueConfig(
                        EMAIL_EXCHANGE,
                        JOB_ALERT_QUEUE,
                        List.of(JOB_ALERT_ROUTING_KEY),
                        JOB_ALERT_DLX,
                        JOB_ALERT_DLQ,
                        JOB_ALERT_DLQ_ROUTING
                ),

                // Follow Company
                new RabbitQueueConfig(
                        FOLLOW_COMPANY_EXCHANGE,
                        FOLLOW_COMPANY_JOB_QUEUE,
                        List.of(FOLLOW_COMPANY_JOB_ROUTING_KEY),
                        FOLLOW_COMPANY_JOB_DLX,
                        FOLLOW_COMPANY_JOB_DLQ,
                        FOLLOW_COMPANY_JOB_DLQ_ROUTING
                ),
                // Job Index
                new RabbitQueueConfig(
                        JOB_INDEX_EXCHANGE,
                        JOB_INDEX_QUEUE,
                        List.of(
                                JOB_INDEX_CREATED_ROUTING_KEY,
                                JOB_INDEX_UPDATED_ROUTING_KEY,
                                JOB_INDEX_DELETED_ROUTING_KEY,
                                JOB_INDEX_RESTORED_ROUTING_KEY,
                                JOB_INDEX_APPLIED_COUNT_INCREMENTED_ROUTING_KEY,
                                JOB_INDEX_APPLICATION_WITHDRAWN_ROUTING_KEY,
                                JOB_INDEX_VIEW_COUNT_INCREMENTED_ROUTING_KEY
                        ),
                        JOB_INDEX_DLX,
                        JOB_INDEX_DLQ,
                        JOB_INDEX_DLQ_ROUTING_KEY
                ),

                // Company Index
                new RabbitQueueConfig(
                        COMPANY_INDEX_EXCHANGE,
                        COMPANY_INDEX_QUEUE,
                        List.of(
                                COMPANY_INDEX_CREATED_ROUTING_KEY,
                                COMPANY_INDEX_UPDATED_ROUTING_KEY,
                                COMPANY_INDEX_DELETED_ROUTING_KEY,
                                COMPANY_INDEX_RESTORED_ROUTING_KEY
                        ),
                        COMPANY_INDEX_DLX,
                        COMPANY_INDEX_DLQ,
                        COMPANY_INDEX_DLQ_ROUTING_KEY
                )

        );

        List<Declarable> declarables = new ArrayList<>();

        for (RabbitQueueConfig config : configs) {

            // Main exchange
            TopicExchange exchange = ExchangeBuilder
                    .topicExchange(config.exchange())
                    .durable(true)
                    .build();

            // DLX
            TopicExchange dlx = ExchangeBuilder
                    .topicExchange(config.dlx())
                    .durable(true)
                    .build();

            // Queue
            Queue queue = QueueBuilder
                    .durable(config.queue())
                    .withArgument(
                            "x-dead-letter-exchange",
                            config.dlx()
                    )
                    .withArgument(
                            "x-dead-letter-routing-key",
                            config.dlqRoutingKey()
                    )
                    .build();

            // DLQ
            Queue dlq = QueueBuilder
                    .durable(config.dlq())
                    .build();

            // Bind all routing keys to the same queue
            for (String routingKey : config.routingKeys()) {
                Binding queueBinding = BindingBuilder
                        .bind(queue)
                        .to(exchange)
                        .with(routingKey);

                declarables.add(queueBinding);
            }

            // DLQ Binding
            Binding dlqBinding = BindingBuilder
                    .bind(dlq)
                    .to(dlx)
                    .with(config.dlqRoutingKey());

            declarables.add(exchange);
            declarables.add(dlx);
            declarables.add(queue);
            declarables.add(dlq);
            declarables.add(dlqBinding);
        }

        return new Declarables(declarables);
    }


    @Bean
    public MessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }


}
