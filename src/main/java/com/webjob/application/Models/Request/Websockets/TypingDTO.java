package com.webjob.application.Models.Request.Websockets;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TypingDTO {
    private String receiverId;
    private String senderId;
    private boolean typing;
}
