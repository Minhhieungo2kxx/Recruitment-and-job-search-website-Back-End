package com.webjob.application.config.Socket;


import com.webjob.application.dto.Response.PresenceEvent;
import com.webjob.application.dto.Response.UserPresenceDTO;
import lombok.AllArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

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
//        messagingTemplate.convertAndSend("/topic/presence", presence);
        messagingTemplate.convertAndSendToUser(
                presence.getUserId().toString(),
                "/queue/presence",
                presence
        );
    }


}
