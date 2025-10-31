package com.webjob.application.Dto.Request.Websockets;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class MessageDeleteDTO {
    private Long messageId;
    private String status;
}
