package com.webjob.application.pubsub.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RedisMessage {
    private String type;

    // userId nếu gửi riêng
    private String userId;

    private String destination;

    private Object payload;
}
