package com.webjob.application.Configs.Socket;

import com.webjob.application.Services.Socket.PresenceService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

@Component
@RequiredArgsConstructor
public class WebSocketPresenceListener {
    private final PresenceService presenceService;
    private final PresenceNotifier notifier; // 👉 inject notifier để broadcast

    @EventListener
    public void handleSessionConnected(SessionConnectEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        String userId = accessor.getFirstNativeHeader("userId"); // client gửi userId ở header

        if (userId != null) {
            Long uid = Long.parseLong(userId);
            presenceService.setUserOnline(uid);
            System.out.println("User " + uid + " online");

            // 👉 Gửi thông báo cho tất cả client
            notifier.notifyStatus(uid, true);
        }
    }

    @EventListener
    public void handleSessionDisconnected(SessionDisconnectEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        String userId = accessor.getFirstNativeHeader("userId");

        if (userId != null) {
            Long uid = Long.parseLong(userId);
            presenceService.setUserOffline(uid);
            System.out.println("User " + uid + " offline");

            // 👉 Gửi thông báo cho tất cả client
            notifier.notifyStatus(uid, false);
        }
    }

}
