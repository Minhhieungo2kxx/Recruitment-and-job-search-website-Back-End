package com.webjob.application.Dto.Request.Websockets;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Data
@Getter
@Setter
public class SeenRequest {
    private Long messageId;
    private Long senderId;      // Người gửi ban đầu
    private Long receiverId;    // Người nhận đã xem
}
