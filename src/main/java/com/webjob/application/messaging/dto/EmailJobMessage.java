package com.webjob.application.messaging.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmailJobMessage  {
    private Long subscriberId;
}
