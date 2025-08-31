package com.webjob.application.Configs.Socket;

import com.webjob.application.Models.Entity.Conversation;
import com.webjob.application.Models.Entity.Message;
import com.webjob.application.Models.Entity.User;
import com.webjob.application.Models.Request.Websockets.UserInfoDTO;
import com.webjob.application.Models.Response.ConversationDTO;
import com.webjob.application.Models.Response.MessagesDTO;
import com.webjob.application.Models.Response.Messensage.MessageResponseDTO;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MessageMapper {
    public MessageResponseDTO toResponseDTO(Message message) {
        if (message == null) {
            return null;
        }
        MessageResponseDTO dto = new MessageResponseDTO();
        dto.setId(message.getId());
        dto.setContent(message.getContent());
        dto.setStatus(message.getStatus());
        dto.setType(message.getType());
        dto.setSender(toUserInfoDTO(message.getSender()));
        dto.setReceiver(toUserInfoDTO(message.getReceiver()));
        dto.setCreatedAt(message.getCreatedAt());
        dto.setUpdatedAt(message.getUpdatedAt());
        dto.setIsEdited(message.getIsEdited());

        return dto;
    }

    public UserInfoDTO toUserInfoDTO(User user) {
        if (user == null) {
            return null;
        }

        UserInfoDTO dto = new UserInfoDTO();
        dto.setId(user.getId());
        dto.setFullName(user.getFullName());
        dto.setEmail(user.getEmail());
        dto.setAvatar(user.getAvatar());

        return dto;
    }
    public ConversationDTO toDTO(Conversation conversation) {
        return new ConversationDTO(
                conversation.getId(),
                conversation.getUser1() != null ? conversation.getUser1().getId() : null,
                conversation.getUser2() != null ? conversation.getUser2().getId() : null,
                conversation.getLastMessage() != null ? conversation.getLastMessage().getId() : null,
                conversation.getCreatedAt(),
                conversation.getUpdatedAt()
        );
    }
    public MessagesDTO toDTO(Message message) {
        return new MessagesDTO(
                message.getId(),
                message.getContent(),
                message.getStatus().name(),
                message.getType().name(),
                message.getSender() != null ? message.getSender().getId() : null,
                message.getReceiver() != null ? message.getReceiver().getId() : null,
                message.getCreatedAt(),
                message.getUpdatedAt(),
                message.getIsDeleted(),
                message.getIsEdited()
        );
    }


}
