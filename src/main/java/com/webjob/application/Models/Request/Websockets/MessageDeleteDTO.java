package com.webjob.application.Models.Request.Websockets;

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
