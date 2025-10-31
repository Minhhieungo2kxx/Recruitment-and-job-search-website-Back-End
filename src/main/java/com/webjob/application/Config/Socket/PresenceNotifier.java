package com.webjob.application.Config.Socket;


import com.webjob.application.Dto.Response.PresenceEvent;
import com.webjob.application.Dto.Response.UserPresenceDTO;
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
