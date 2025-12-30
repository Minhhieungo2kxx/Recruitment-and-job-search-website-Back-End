package com.webjob.application.Controller;

import com.webjob.application.Annotation.RateLimit;
import com.webjob.application.Dto.Request.Websockets.SeenRequest;
import com.webjob.application.Model.Entity.Message;
import com.webjob.application.Model.Entity.User;
import com.webjob.application.Dto.Request.Websockets.MessageRequestDTO;
import com.webjob.application.Dto.Response.Messensage.MessageResponseDTO;
import com.webjob.application.Service.Socket.MessageService;
import com.webjob.application.Service.UserService;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Controller;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.security.Principal;
import java.util.Map;

@Controller
public class WebSocketController {
    private final MessageService messageService;
    private final SimpMessagingTemplate messagingTemplate;

    private final UserService userService;

    public WebSocketController(MessageService messageService,
                               SimpMessagingTemplate messagingTemplate, UserService userService) {
        this.messageService = messageService;
        this.messagingTemplate = messagingTemplate;
        this.userService = userService;
    }
    @RateLimit(maxRequests = 20, timeWindowSeconds = 60, keyType = "TOKEN")
    @MessageMapping("/chat.sendMessage")
    @SendTo("/topic/public")
    public MessageResponseDTO sendMessage(@Payload MessageRequestDTO messageRequest,
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
    @MessageMapping("/chat.seen")
    public void seenMessage(SeenRequest seenRequest, Principal principal) {
        Long messageId = seenRequest.getMessageId();   //  ĐÚNG
        Long senderId  = seenRequest.getSenderId();    // người gửi
        Long receiverId = seenRequest.getReceiverId(); // người xem
        messageService.markMessageAsRead(receiverId, senderId);

        MessageResponseDTO updatedMessage =
                messageService.getMessageById(messageId);

        messagingTemplate.convertAndSendToUser(
                updatedMessage.getSender().getEmail(),
                "/queue/message-status",
                updatedMessage
        );
    }





}
