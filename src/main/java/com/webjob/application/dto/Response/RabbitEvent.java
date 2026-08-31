package com.webjob.application.dto.Response;

import lombok.*;

import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RabbitEvent <T>{
    private String eventId;

    private String eventType;

    private String aggregateType;

    private String aggregateId;

    private T payload;
}
