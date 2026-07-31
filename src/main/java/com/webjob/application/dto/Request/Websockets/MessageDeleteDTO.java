package com.webjob.application.dto.Request.Websockets;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class MessageDeleteDTO {
    private Long messageId;
    private String status;
    private Long deletedByUserId;

    private Long senderId;
    private Long receiverId;
}
