package com.webjob.application.Configs.Socket;


import com.webjob.application.Models.Response.PresenceEvent;
import com.webjob.application.Models.Response.UserPresenceDTO;
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
                event.getPresence()
        );
    }

    // Broadcast trạng thái cho specific user
    public void notifyUserPresence(UserPresenceDTO presence) {
        messagingTemplate.convertAndSend("/topic/presence", presence);
    }

    // Gửi trạng thái cho user cụ thể
    public void sendPresenceToUser(Long targetUserId, UserPresenceDTO presence) {
        messagingTemplate.convertAndSendToUser(
                targetUserId.toString(),
                "/queue/presence",
                presence
        );
    }
}
