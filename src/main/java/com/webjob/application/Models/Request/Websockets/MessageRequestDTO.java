package com.webjob.application.Models.Request.Websockets;

import com.webjob.application.Models.Entity.Message;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MessageRequestDTO {
    @NotNull(message = "ID người nhận không được để trống")
    private Long receiverId;

    @NotBlank(message = "Nội dung tin nhắn không được để trống")
    private String content;

    private Message.MessageType type = Message.MessageType.CHAT;
}
