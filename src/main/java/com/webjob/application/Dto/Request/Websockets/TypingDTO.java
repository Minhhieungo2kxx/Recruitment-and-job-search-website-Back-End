package com.webjob.application.Dto.Request.Websockets;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TypingDTO {
    private String receiverId;
    private String senderId;
    private boolean typing;
}
