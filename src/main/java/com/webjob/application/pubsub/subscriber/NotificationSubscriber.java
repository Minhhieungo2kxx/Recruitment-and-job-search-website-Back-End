package com.webjob.application.pubsub.subscriber;


import com.webjob.application.pubsub.dto.RedisMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationSubscriber {
    private final SimpMessagingTemplate messagingTemplate;

    public void receiveMessage(RedisMessage message) {
        log.info("Nhan tin nhan tu Redis cho userId: {}", message.getUserId());
        try {
            messagingTemplate.convertAndSendToUser(
                    message.getUserId(),
                    message.getDestination(),
                    message.getPayload()
            );
            log.info("Gui WebSocket thanh cong toi userId: {}", message.getUserId());
        } catch (Exception e) {
            log.error("Loi khi gui WebSocket toi userId: {}", message.getUserId(), e);
        }
    }
}

