package com.webjob.application.Configs.Socket;

import com.webjob.application.Models.Enums.PresenceEvent;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@AllArgsConstructor
public class PresenceNotifier {
    private final SimpMessagingTemplate messagingTemplate;

    @EventListener
    public void handlePresenceChange(PresenceEvent event) {
        messagingTemplate.convertAndSend(
                "/topic/presence",
                Map.of("userId", event.userId(), "online", event.online())
        );
    }
    public void notifyStatus(Long userId, boolean isOnline) {
        messagingTemplate.convertAndSend("/topic/presence",
                new PresenceStatus(userId, isOnline));
    }
    @Data
    @AllArgsConstructor
    public static class PresenceStatus {
        private Long userId;
        private boolean online;
    }
}
