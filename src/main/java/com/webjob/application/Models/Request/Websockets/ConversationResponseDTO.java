package com.webjob.application.Models.Request.Websockets;

import com.webjob.application.Models.Response.Messensage.MessageResponseDTO;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ConversationResponseDTO {
    private Long id;
    private UserInfoDTO otherUser;
    private MessageResponseDTO lastMessage;
    private Instant updatedAt;
    private Long unreadCount;
}
