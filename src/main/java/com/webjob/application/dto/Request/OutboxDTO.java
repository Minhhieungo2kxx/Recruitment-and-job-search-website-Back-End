package com.webjob.application.dto.Request;

import com.webjob.application.enums.OutboxCategory;
import com.webjob.application.enums.OutboxEventType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OutboxDTO {

    private String aggregateType;
    private String aggregateId;
    private OutboxCategory category;
    private OutboxEventType eventType;
    private String exchangeName;
    private String routingKey;
    private Object payload;


}
