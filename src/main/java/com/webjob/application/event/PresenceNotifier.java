package com.webjob.application.event;


import com.webjob.application.dto.Response.PresenceEvent;
import com.webjob.application.dto.Response.UserPresenceDTO;
import com.webjob.application.pubsub.dto.RedisMessage;
import com.webjob.application.pubsub.publisher.RedisPublisher;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
@Slf4j
public class PresenceNotifier {
    private final SimpMessagingTemplate messagingTemplate;
    private final RedisPublisher redisPublisher;

    @EventListener
    public void handlePresenceChange(PresenceEvent event) {
        RedisMessage redisMessage = RedisMessage.builder()
                .type("PRESENCE_CHANGE")
                .destination("/topic/presence")
                .payload(event.getPresence())
                .build();
        redisPublisher.publish("presence",redisMessage);
        log.info(" Đang publish sự kiện Presence lên Redis - Destination: {}", redisMessage.getDestination());
    }

    // Broadcast trạng thái cho specific user
    @EventListener
    public void notifyUserPresence(UserPresenceDTO presence) {

        RedisMessage redisMessage = RedisMessage.builder()
                .type("USER_PRESENCE")
                .userId(presence.getUserId().toString())
                .destination("/queue/presence")
                .payload(presence)
                .build();

        redisPublisher.publish("presence", redisMessage);
    }


}
