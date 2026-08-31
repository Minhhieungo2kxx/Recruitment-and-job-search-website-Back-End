package com.webjob.application.dto.record;

import java.util.List;

public record RabbitQueueConfig(
        String exchange,
        String queue,
//        String routingKey,
        List<String> routingKeys,
        String dlx,
        String dlq,
        String dlqRoutingKey
) {
}
