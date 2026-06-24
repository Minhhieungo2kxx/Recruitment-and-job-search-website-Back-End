package com.webjob.application.dto.Response.Messensage;

import com.webjob.application.models.Entity.Message;
import com.webjob.application.dto.Request.Websockets.UserInfoDTO;
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
    // THÊM CÁC FIELD MỚI
    private Message.MessageContentType contentType;
    private String fileUrl;
    private String fileName;
    private Long fileSize;

    private UserInfoDTO sender;
    private UserInfoDTO receiver;
    private Instant createdAt;
    private Instant updatedAt;
    private Boolean isEdited;
    private Boolean isDeleted;
}
