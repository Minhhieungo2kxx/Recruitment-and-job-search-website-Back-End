package com.webjob.application.pubsub.subscriber;

import com.webjob.application.dto.Request.Websockets.MessageDeleteDTO;
import com.webjob.application.dto.Response.Messensage.MessageResponseDTO;
import com.webjob.application.pubsub.dto.RedisMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChatPrivateSubscriber {
    private final SimpMessagingTemplate messagingTemplate;

    public void receiveMessage(RedisMessage message){

        switch (message.getType()) {

            case "MESSAGE_SEEN":
                sendToUserSpecificWebSocket(message); // Tận dụng lại hàm gửi tới user cụ thể
                break;

            case "CHAT_MESSAGE":

                sendChatMessage(message);
                break;

            case "UPDATE_MESSAGE":

                sendChatMessage(message);
                break;

            case "DELETE_MESSAGE":

                sendChatMessageDelete(message);
                break;

            default:
                log.warn("Không rõ type");
                log.warn("Không nhận diện được Message Type: {}.", message.getType());
                break;
        }
    }

    private void sendToUserSpecificWebSocket(RedisMessage message) {
        try {
            messagingTemplate.convertAndSendToUser(
                    message.getUserId(),
                    message.getDestination(),
                    message.getPayload()
            );
            log.info(" Đã đẩy message thành công tới destination: {}", message.getDestination());
        } catch (Exception e) {
            log.error("Lỗi khi gửi message đến user {}: {}", message.getUserId(), e.getMessage());
        }
    }

    public void sendChatMessage(RedisMessage message) {
        MessageResponseDTO chatMessage = (MessageResponseDTO) message.getPayload();

        if (chatMessage == null || chatMessage.getReceiver() == null || chatMessage.getSender() == null) {
            log.warn("Không thể gửi tin nhắn vì thông tin tin nhắn, người nhận hoặc người gửi bị null.");
            return;
        }
        String receiverId = chatMessage.getReceiver().getId().toString();
        String senderId = chatMessage.getSender().getId().toString();

        // 1. Gửi tin nhắn cho người nhận
        log.info("Đang gửi tin nhắn tới người nhận [ID: {}] qua kênh /queue/messages", receiverId);
        messagingTemplate.convertAndSendToUser(
                receiverId,
                message.getDestination(),
                chatMessage
        );

        // 2. Gửi lại tin nhắn cho chính người gửi
        log.info("Đang gửi lại bản sao tin nhắn cho người gửi [ID: {}] qua kênh /queue/messages", senderId);
        messagingTemplate.convertAndSendToUser(
                senderId,
                message.getDestination(),
                chatMessage
        );
        log.debug("Đã hoàn tất quá trình phân phối tin nhắn cho ID người gửi: {} và người nhận: {}", senderId, receiverId);
    }

    public void sendChatMessageDelete(RedisMessage message) {
        MessageDeleteDTO chatMessage = (MessageDeleteDTO) message.getPayload();

        String receiverId =chatMessage.getReceiverId().toString();
        String senderId = chatMessage.getSenderId().toString();

        // 1. Gửi lại tin nhắn cho chính người gửi

        messagingTemplate.convertAndSendToUser(
                senderId,
                message.getDestination(),
                chatMessage
        );

        // 2. Gửi tin nhắn cho người nhận

        messagingTemplate.convertAndSendToUser(
                receiverId,
                message.getDestination(),
                chatMessage
        );

    }


}
