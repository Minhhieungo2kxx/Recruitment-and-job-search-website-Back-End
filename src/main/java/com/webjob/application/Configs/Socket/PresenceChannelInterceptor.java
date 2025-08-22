package com.webjob.application.Configs.Socket;

import com.webjob.application.Models.Enums.PresenceEvent;
import com.webjob.application.Services.Socket.PresenceService;
import lombok.RequiredArgsConstructor;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Lazy;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PresenceChannelInterceptor implements ChannelInterceptor {
    private final PresenceService presenceService;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor =
                MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null) return message;

        StompCommand cmd = accessor.getCommand();
        if (StompCommand.CONNECT.equals(cmd)) {
            String userIdHeader = accessor.getFirstNativeHeader("userId");
            Long userId = (userIdHeader != null) ? Long.parseLong(userIdHeader) : null;

            if (userId != null) {
                boolean becameOnline = presenceService.addSession(userId, accessor.getSessionId());
                if (becameOnline) {
                    eventPublisher.publishEvent(new PresenceEvent(userId, true));
                }
            }
        } else if (StompCommand.DISCONNECT.equals(cmd)) {
            Long offlineUser = presenceService.removeSession(accessor.getSessionId());
            if (offlineUser != null) {
                eventPublisher.publishEvent(new PresenceEvent(offlineUser, false));
            }
        }
        return message;
    }

}
