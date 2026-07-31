package com.webjob.application.pubsub.subscriber;


import com.webjob.application.pubsub.dto.RedisMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class PresenceSubscriber {
    private final SimpMessagingTemplate messagingTemplate;

    public void receiveMessage(RedisMessage message){

        switch (message.getType()) {
            case "PRESENCE_CHANGE":
                sendToWebSocket(message);
                break;

            //  gửi riêng đến từng user
            case "USER_PRESENCE":
                sendToUserSpecificWebSocket(message);
                break;

            default:
                log.warn("Không rõ type");
                log.warn(" Không nhận diện được Message Type: {}.", message.getType());
                break;
        }

    }

    private void sendToWebSocket(RedisMessage message) {
        messagingTemplate.convertAndSend(
                message.getDestination(),
                message.getPayload()
        );
        log.info(" Đã đẩy message thành công tới destination: {}", message.getDestination());
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
}
