package com.webjob.application.dto.Response;

import com.webjob.application.enums.SubscriberStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class SubscriberListResponse {
    private Long id;

    private String name;

    private String email;

    private SubscriberStatus status;
}
