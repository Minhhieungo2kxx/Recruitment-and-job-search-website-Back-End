package com.webjob.application.messaging.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

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


    //chung
    @Bean
    public TopicExchange emailExchange() {
        return ExchangeBuilder
                .topicExchange(EMAIL_EXCHANGE)
                .durable(true)
                .build();
    }

    //    cron subscriberId job with skill
    @Bean
    public TopicExchange deadLetterExchange() {
        return new TopicExchange(DLX_EXCHANGE);
    }

    @Bean
    public Queue emailQueue() {
        return QueueBuilder
                .durable(EMAIL_QUEUE)
                .withArgument("x-dead-letter-exchange", DLX_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", DLQ_ROUTING_KEY)
                .build();
    }

    @Bean
    public Queue deadLetterQueue() {
        return QueueBuilder
                .durable(DLQ_QUEUE)
                .build();
    }

    @Bean
    public Binding emailBinding() {
        return BindingBuilder.bind(emailQueue())
                .to(emailExchange())
                .with(EMAIL_ROUTING_KEY);
    }

    @Bean
    public Binding deadBinding() {
        return BindingBuilder.bind(deadLetterQueue())
                .to(deadLetterExchange())
                .with(DLQ_ROUTING_KEY);
    }

    @Bean
    public MessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }

//    config cho // Forgot password

    @Bean
    TopicExchange forgotDeadExchange() {
        return new TopicExchange(FORGOT_DLX);
    }

    @Bean
    Queue forgotQueue() {
        return QueueBuilder.durable(FORGOT_QUEUE)
                .withArgument("x-dead-letter-exchange", FORGOT_DLX)
                .withArgument("x-dead-letter-routing-key", FORGOT_DLQ_ROUTING)
                .build();
    }

    @Bean
    Queue forgotDeadQueue() {
        return QueueBuilder.durable(FORGOT_DLQ).build();
    }

    @Bean
    Binding forgotBinding() {
        return BindingBuilder.bind(forgotQueue())
                .to(emailExchange())
                .with(FORGOT_ROUTING_KEY);
    }

    @Bean
    Binding forgotDeadBinding() {
        return BindingBuilder.bind(forgotDeadQueue())
                .to(forgotDeadExchange())
                .with(FORGOT_DLQ_ROUTING);
    }

    //Config JOB_APPLY
    @Bean
    public TopicExchange jobApplyDlx() {
        return ExchangeBuilder
                .topicExchange(JOB_APPLY_DLX)
                .durable(true)
                .build();
    }


    @Bean
    public Queue jobApplyQueue() {
        return QueueBuilder
                .durable(JOB_APPLY_QUEUE)
                .withArgument(
                        "x-dead-letter-exchange",
                        JOB_APPLY_DLX
                )
                .withArgument(
                        "x-dead-letter-routing-key",
                        JOB_APPLY_DLQ_ROUTING
                )
                .build();
    }


    @Bean
    public Queue jobApplyDlq() {
        return QueueBuilder
                .durable(JOB_APPLY_DLQ)
                .build();
    }


    @Bean
    public Binding bindingJobApplyQueue() {
        return BindingBuilder
                .bind(jobApplyQueue())
                .to(emailExchange())
                .with(JOB_APPLY_ROUTING_KEY);
    }


    @Bean
    public Binding bindingJobApplyDlq() {
        return BindingBuilder
                .bind(jobApplyDlq())
                .to(jobApplyDlx())
                .with(JOB_APPLY_DLQ_ROUTING);
    }

    //    Config Job Alert
    @Bean
    public TopicExchange jobAlertDlx() {
        return ExchangeBuilder
                .topicExchange(JOB_ALERT_DLX)
                .durable(true)
                .build();
    }

    @Bean
    public Queue jobAlertQueue() {
        return QueueBuilder
                .durable(JOB_ALERT_QUEUE)
                .withArgument(
                        "x-dead-letter-exchange",
                        JOB_ALERT_DLX
                )
                .withArgument(
                        "x-dead-letter-routing-key",
                        JOB_ALERT_DLQ_ROUTING
                )
                .build();
    }

    @Bean
    public Queue jobAlertDlq() {
        return QueueBuilder
                .durable(JOB_ALERT_DLQ)
                .build();
    }

    @Bean
    public Binding bindingJobAlertQueue() {
        return BindingBuilder
                .bind(jobAlertQueue())
                .to(emailExchange())
                .with(JOB_ALERT_ROUTING_KEY);
    }

    @Bean
    public Binding bindingJobAlertDlq() {
        return BindingBuilder
                .bind(jobAlertDlq())
                .to(jobAlertDlx())
                .with(JOB_ALERT_DLQ_ROUTING);
    }


}
