package com.webjob.application.pubsub.subscriber;

import com.webjob.application.pubsub.dto.RedisMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChatPublicSubscriber {

    private final SimpMessagingTemplate messagingTemplate;

    public void receiveMessage(RedisMessage message){

        switch (message.getType()) {

            case "USER_LEAVE":
                sendToWebSocket(message);
                break;

            default:
                log.warn("Không rõ type");
                log.warn("Không nhận diện được Message Type: {}.", message.getType());
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
}
