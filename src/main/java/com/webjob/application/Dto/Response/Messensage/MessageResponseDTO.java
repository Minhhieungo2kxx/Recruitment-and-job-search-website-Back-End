package com.webjob.application.Dto.Response.Messensage;

import com.webjob.application.Model.Entity.Message;
import com.webjob.application.Dto.Request.Websockets.UserInfoDTO;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MessageResponseDTO {
    private Long id;
    private String content;
    private Message.MessageStatus status;
    private Message.MessageType type;
    private UserInfoDTO sender;
    private UserInfoDTO receiver;
    private Instant createdAt;
    private Instant updatedAt;
    private Boolean isEdited;
}
