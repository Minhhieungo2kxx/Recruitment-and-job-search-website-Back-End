package com.webjob.application.service.Socket;

import com.webjob.application.dto.Request.Websockets.MessageRequestDTO;
import com.webjob.application.dto.Request.Websockets.SeenRequest;
import com.webjob.application.dto.Response.Messensage.MessageResponseDTO;
import com.webjob.application.models.Entity.Message;
import com.webjob.application.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.security.Principal;

@Service
@RequiredArgsConstructor
public class WebsocketService {
    private final MessageService messageService;
    private final SimpMessagingTemplate messagingTemplate;

    private final UserService userService;

    public MessageResponseDTO sendMessage( MessageRequestDTO messageRequest,
                                          Principal principal) {
        return messageService.sendMessage(principal.getName(), messageRequest);
    }
    @EventListener
    public void handleWebSocketConnectListener(SessionConnectedEvent event) {
        System.out.println("Received a new web socket connection");
    }
    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String username = (String) headerAccessor.getSessionAttributes().get("username");

        if (username != null) {
            System.out.println("User disconnected: " + username);

            MessageRequestDTO leaveMessage = new MessageRequestDTO();
            leaveMessage.setType(Message.MessageType.LEAVE);
            leaveMessage.setContent(username + " đã rời khỏi cuộc trò chuyện");

            messagingTemplate.convertAndSend("/topic/public", leaveMessage);
        }
    }
    public void seenMessage(SeenRequest seenRequest) {
        Long messageId = seenRequest.getMessageId();   //  ĐÚNG
        Long senderId  = seenRequest.getSenderId();    // người gửi
        Long receiverId = seenRequest.getReceiverId(); // người xem
        messageService.markMessageAsRead(receiverId, senderId);

        MessageResponseDTO updatedMessage =
                messageService.getMessageById(messageId);

        messagingTemplate.convertAndSendToUser(
                updatedMessage.getSender().getId().toString(),
                "/queue/message-status",
                updatedMessage
        );
    }


}
