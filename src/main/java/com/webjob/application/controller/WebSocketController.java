package com.webjob.application.controller;

import com.webjob.application.annotation.RateLimit;
import com.webjob.application.dto.Request.Websockets.SeenRequest;
import com.webjob.application.models.Entity.Message;
import com.webjob.application.dto.Request.Websockets.MessageRequestDTO;
import com.webjob.application.dto.Response.Messensage.MessageResponseDTO;
import com.webjob.application.service.Socket.MessageService;
import com.webjob.application.service.Socket.WebsocketService;
import com.webjob.application.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Controller;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.security.Principal;

@Controller
@RequiredArgsConstructor
public class WebSocketController {
   private final WebsocketService websocketService;


    @MessageMapping("/chat.sendMessage")
    @SendTo("/topic/public")
    public MessageResponseDTO sendMessage(@Payload MessageRequestDTO messageRequest,
                                          Principal principal) {
        return websocketService.sendMessage(messageRequest,principal);

    }



    @MessageMapping("/chat.seen")
    public void seenMessage(SeenRequest seenRequest, Principal principal) {
        websocketService.seenMessage(seenRequest);
    }




}
