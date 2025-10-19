package com.webjob.application.Controller;

import com.webjob.application.Annotation.RateLimit;
import com.webjob.application.Models.Entity.Message;
import com.webjob.application.Models.Entity.User;
import com.webjob.application.Models.Request.Websockets.MessageRequestDTO;
import com.webjob.application.Models.Request.Websockets.TypingDTO;
import com.webjob.application.Models.Response.Messensage.MessageResponseDTO;
import com.webjob.application.Services.Socket.MessageService;
import com.webjob.application.Services.UserService;
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
    @RateLimit(maxRequests = 5, timeWindowSeconds = 60, keyType = "IP")
    @MessageMapping("/chat.addUser")
    @SendTo("/topic/public")
    public MessageResponseDTO addUser(@Payload MessageRequestDTO messageRequest,
                                      Principal principal) {
        messageRequest.setType(Message.MessageType.JOIN);
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
    @RateLimit(maxRequests = 10, timeWindowSeconds = 60, keyType = "TOKEN")
    @MessageMapping("/call.signal")
    public void handleCallSignal(@Payload Map<String, Object> signal, Principal principal) {
        String receiverId = (String) signal.get("receiverId");
        User user=userService.getbyID(Long.parseLong(receiverId))
                .orElseThrow(()->new UsernameNotFoundException("Not found with "+receiverId));
        if (user!=null){
            messagingTemplate.convertAndSendToUser(user.getEmail(), "/queue/call", signal);
        }
        else {
            System.out.println("Không tìm thấy user với ID: " + receiverId);
        }

        System.out.println("Sending call signal to user: " + receiverId);

    }
    @RateLimit(maxRequests = 30, timeWindowSeconds = 60, keyType = "TOKEN")
    @MessageMapping("/call.candidate")
    public void handleCallCandidate(@Payload Map<String, Object> candidate, Principal principal) {
        String receiverId = (String) candidate.get("receiverId");
        User user=userService.getbyID(Long.parseLong(receiverId))
                .orElseThrow(()->new UsernameNotFoundException("Not found with "+receiverId));
        messagingTemplate.convertAndSendToUser(user.getEmail(), "/queue/call", candidate);
        System.out.println("Sending call signal to user: " + receiverId);

    }


}
