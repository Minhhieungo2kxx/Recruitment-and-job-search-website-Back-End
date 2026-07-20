package com.webjob.application.service.Socket;

import com.webjob.application.config.Socket.MessageMapper;
import com.webjob.application.dto.Request.Websockets.*;
import com.webjob.application.dto.Response.ApiResponse;
import com.webjob.application.models.Entity.Conversation;
import com.webjob.application.models.Entity.Message;
import com.webjob.application.models.Entity.User;
import com.webjob.application.dto.Request.Search.MessageFilterRequest;
import com.webjob.application.dto.Response.MessagesDTO;
import com.webjob.application.dto.Response.Messensage.MessageResponseDTO;
import com.webjob.application.dto.Response.MetaDTO;
import com.webjob.application.dto.Response.ResponseDTO;
import com.webjob.application.repository.ConversationRepository;
import com.webjob.application.repository.MessageRepository;
import com.webjob.application.repository.UserRepository;
import com.webjob.application.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class MessageService {
    private final MessageRepository messageRepository;
    private final ConversationRepository conversationRepository;
    private final UserRepository userRepository;
    private final UserService userService;
    private final MessageMapper messageMapper;
    private final SimpMessagingTemplate messagingTemplate;
    @Value("${upload.base-dir}")
    private String uploadBaseDir;


    public MessageResponseDTO sendMessage(String userID, MessageRequestDTO requestDTO) {
        User sender = userService.getById(Long.valueOf(userID));

        User receiver = userRepository.findById(requestDTO.getReceiverId())
                .orElseThrow(() -> new RuntimeException("Người nhận không tồn tại"));

        Message message = new Message();
        message.setContent(requestDTO.getContent());
        message.setSender(sender);
        message.setReceiver(receiver);
        message.setType(requestDTO.getType());
        message.setStatus(Message.MessageStatus.SENT);
        // XỬ LÝ FILE NẾU CÓ
        if (requestDTO.getContentType() != null) {
            message.setContentType(requestDTO.getContentType());
        }
        if (requestDTO.getFileUrl() != null && !requestDTO.getFileUrl().isEmpty()) {
            message.setFileUrl(requestDTO.getFileUrl());
            message.setFileName(requestDTO.getFileName());
            message.setFileSize(requestDTO.getFileSize());
        }
        Message savedMessage = messageRepository.save(message);

        // Cập nhật hoặc tạo conversation
        updateOrCreateConversation(sender, receiver, savedMessage);

        return messageMapper.toResponseDTO(savedMessage);
    }

    public MessageResponseDTO updateMessage(String userID, MessageUpdateDTO updateDTO) {
        Message message = messageRepository.findById(updateDTO.getMessageId())
                .orElseThrow(() -> new RuntimeException("Tin nhắn không tồn tại"));
        User user = userService.getById(Long.valueOf(userID));
        if (message.getSender().getId() != user.getId()) {
            throw new RuntimeException("Bạn chỉ có thể sửa tin nhắn của mình");
        }
        // KHÔNG CHO SỬA MESSAGE CÓ FILE
        if (message.getContentType() != Message.MessageContentType.TEXT) {
            throw new RuntimeException("Không thể sửa tin nhắn có file đính kèm");
        }
        message.setContent(updateDTO.getContent());
        message.setIsEdited(true);
        Message updatedMessage = messageRepository.save(message);

        return messageMapper.toResponseDTO(updatedMessage);
    }

    public MessageResponseDTO getMessageById(Long messageId) {
        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new RuntimeException("Tin nhắn không tồn tại"));
        return messageMapper.toResponseDTO(message);
    }

    @Transactional
    public void deleteMessage(String userID, Long messageId) {
        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new RuntimeException("Tin nhắn không tồn tại"));

        User user = userService.getById(Long.valueOf(userID));

        if (message.getSender().getId() != user.getId()) {
            throw new RuntimeException("Bạn chỉ có thể sửa tin nhắn của mình");
        }


        // XÓA FILE NẾU CÓ
        if (message.getFileUrl() != null && !message.getFileUrl().isEmpty() &&
                (message.getContentType() == Message.MessageContentType.IMAGE
                        || message.getContentType() == Message.MessageContentType.FILE)) {
            deleteFileFromStorage(message.getFileUrl());
            log.info("User {} xóa message {} kèm file {}", user.getEmail(), messageId, message.getFileUrl());

        }
        message.setIsDeleted(true);
        message.setContent("Tin nhắn đã bị xóa");
        message.setFileUrl(null);
        message.setFileName(null);
        message.setFileSize(null);
        messageRepository.save(message);

        // Gửi thông báo xóa tin nhắn cho cả sender và receiver
        MessageDeleteDTO deleteDTO = new MessageDeleteDTO(message.getId(), "Tin nhắn đã bị xóa",
                user.getId());

        messagingTemplate.convertAndSendToUser(
                message.getSender().getId().toString(),
                "/queue/message-deletes",
                deleteDTO
        );

        messagingTemplate.convertAndSendToUser(
                message.getReceiver().getId().toString(),
                "/queue/message-deletes",
                deleteDTO
        );
    }

    @Transactional
    public List<MessageResponseDTO> getMessagesBetweenUsers(String userId, Long otherUserId) {
        User user = userService.getById(Long.valueOf(userId));
        List<Message> messages = messageRepository.findMessagesBetweenUsers(user.getId(), otherUserId);

        // Đánh dấu tin nhắn là đã đọc
        messageRepository.markMessagesAsRead(user.getId(), otherUserId);

        return messages.stream()
                .map(messageMapper::toResponseDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public List<ConversationResponseDTO> getUserConversations(String userID) {
        User user = userService.getById(Long.valueOf(userID));

        List<Conversation> conversations = conversationRepository.findConversationsByUserId(user.getId());

        return conversations.stream()
                .map(conversation -> {
                    User otherUser = conversation.getUser1().getId() == user.getId() ?
                            conversation.getUser2() : conversation.getUser1();

                    Long unreadCount = messageRepository.countUnreadMessagesBetweenUsers(
                            user.getId(), otherUser.getId());

                    ConversationResponseDTO dto = new ConversationResponseDTO();
                    dto.setId(conversation.getId());
                    dto.setOtherUser(messageMapper.toUserInfoDTO(otherUser));
                    dto.setUpdatedAt(conversation.getUpdatedAt());
                    dto.setUnreadCount(unreadCount);

                    if (conversation.getLastMessage() != null) {
                        dto.setLastMessage(messageMapper.toResponseDTO(conversation.getLastMessage()));
                    }

                    return dto;
                })
                .collect(Collectors.toList());
    }

//    @Transactional(readOnly = true)
//    public List<UserInfoDTO> searchUsers(String userID, String searchTerm) {
//        User currentUser = userService.getById(Long.valueOf(userID));
//        List<User> users = isHR(currentUser)
//                ? userRepository.findCandidatesByName(searchTerm)
//                : userRepository.findHRsByName(searchTerm);
//
//        return users.stream()
//                .map(messageMapper::toUserInfoDTO)
//                .collect(Collectors.toList());
//    }
@Transactional(readOnly = true)
public List<UserInfoDTO> searchUsers(String userID, String searchTerm) {

    User currentUser = userService.getById(Long.valueOf(userID));
    String roleGroup = getRoleGroup(currentUser);

    List<User> users;

    if ("HR".equals(roleGroup)) {
        users = userRepository.findCandidatesByName(searchTerm);
    } else if ("USER".equals(roleGroup)) {
        users = userRepository.findHRsByName(searchTerm);
    } else {
        users = Collections.emptyList();
    }

    return users.stream()
            .map(messageMapper::toUserInfoDTO)
            .collect(Collectors.toList());
}

//    private boolean isHR(User user) {
//        return "HR".equals(getRoleGroup(user));
//    }
    private String getRoleGroup(User user) {
        String code = user.getRole().getCode();

        if (code.startsWith("HR")) return "HR";
        if (code.startsWith("ADMIN")) return "ADMIN";
        if (code.equals("USER")) return "USER";

        return "UNKNOWN";
    }


    private void updateOrCreateConversation(User user1, User user2, Message lastMessage) {
        Optional<Conversation> existingConversation = conversationRepository
                .findConversationBetweenUsers(user1.getId(), user2.getId());

        if (existingConversation.isPresent()) {
            Conversation conversation = existingConversation.get();
            conversation.setLastMessage(lastMessage);
            conversationRepository.save(conversation);
        } else {
            Conversation newConversation = new Conversation();
            newConversation.setUser1(user1);
            newConversation.setUser2(user2);
            newConversation.setLastMessage(lastMessage);
            conversationRepository.save(newConversation);
        }
    }


    public ResponseDTO<List<MessagesDTO>> getPaginated(int page,int size,MessageFilterRequest filterRequest) {
        if(filterRequest==null){
            filterRequest=new MessageFilterRequest();
        }

        size = Math.min(Math.max(size, 1), 50);
        page = Math.max(page, 1);
        Pageable pageable = PageRequest.of(page - 1, size, Sort.by("createdAt").descending());
        Page<Message> messages;

        if (filterRequest.getStatus() != null) {
            messages = messageRepository.findAllByStatus(Message.MessageStatus.valueOf(filterRequest.getStatus()), pageable);
        } else if (filterRequest.getType() != null) {
            messages = messageRepository.findAllByType(Message.MessageType.valueOf(filterRequest.getType()), pageable);
        } else if (filterRequest.getIsDeleted() != null) {
            messages = messageRepository.findAllByIsDeleted(filterRequest.getIsDeleted(), pageable);
        } else {
            messages = messageRepository.findAll(pageable);
        }
        int currentpage = messages.getNumber() + 1;
        int pagesize = messages.getSize();
        int totalpage = messages.getTotalPages();
        Long totalItem = messages.getTotalElements();

        MetaDTO metaDTO = new MetaDTO(currentpage, pagesize, totalpage, totalItem);
        List<Message> list = messages.getContent();
        List<MessagesDTO> listmessage = list.stream().map(messageMapper::toDTO).collect(Collectors.toList());
        ResponseDTO<List<MessagesDTO>> respond = new ResponseDTO<>(metaDTO, listmessage);
        return respond;

    }

    public boolean softDeleteMessage(Long id) {
        return messageRepository.findById(id).map(message -> {
            message.setIsDeleted(true);
            messageRepository.save(message);
            return true;
        }).orElse(false);
    }

    @Transactional
    public void markMessageAsRead(Long receiverId, Long senderId) {
        messageRepository.markMessagesAsRead(receiverId, senderId);
    }

    // PHƯƠNG THỨC XÓA FILE
    private void deleteFileFromStorage(String fileName) {
        try {
            Path filePath = Paths.get(uploadBaseDir, "chat-files", fileName);

            if (Files.exists(filePath)) {
                Files.delete(filePath);
                log.info("Đã xóa file: {}", filePath);
            } else {
                log.warn("File không tồn tại: {}", filePath);
            }

        } catch (IOException e) {
            log.error("Lỗi khi xóa file: {}", fileName, e);
        }
    }

//    public ResponseEntity<?> getAllMessages(int page,int size,MessageFilterRequest filterRequest) {
//        ResponseDTO<?> respond = getPaginated(page,size,filterRequest);
//
//    }
    public ResponseDTO<List<MessagesDTO>> getAllMessages(int page,int size,MessageFilterRequest filterRequest) {
        ResponseDTO<List<MessagesDTO>> respond = getPaginated(page,size,filterRequest);
        return respond;
    }

    public ResponseEntity<Void> deleteMessage(Long id) {
        boolean deleted = softDeleteMessage(id);
        return deleted ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build();
    }


    @Transactional
    public  MessageResponseDTO sendMessage(MessageRequestDTO requestDTO, Authentication authentication) {
        MessageResponseDTO message = sendMessage(authentication.getName(), requestDTO);
        // Gửi tin nhắn qua WebSocket đến người nhận
        messagingTemplate.convertAndSendToUser(
                message.getReceiver().getId().toString(),
                "/queue/messages",
                message
        );
        // Gửi lại tin nhắn cho chính người gửi
        messagingTemplate.convertAndSendToUser(
                message.getSender().getId().toString(),
                "/queue/messages",
                message
        );
       return message;
    }


    @Transactional
    public MessageResponseDTO updateMessage(MessageUpdateDTO updateDTO, Authentication authentication) {

        MessageResponseDTO updatedMessage = updateMessage(authentication.getName(), updateDTO);

        // Thông báo cập nhật qua WebSocket
        messagingTemplate.convertAndSendToUser(
                updatedMessage.getReceiver().getId().toString(),
                "/queue/message-updates",
                updatedMessage
        );
        messagingTemplate.convertAndSendToUser(
                updatedMessage.getSender().getId().toString(),
                "/queue/message-updates",
                updatedMessage
        );
        return updatedMessage;

    }


}
