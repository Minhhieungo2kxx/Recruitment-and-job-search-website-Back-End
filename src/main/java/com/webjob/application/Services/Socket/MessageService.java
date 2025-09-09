package com.webjob.application.Services.Socket;

import com.webjob.application.Configs.Socket.MessageMapper;
import com.webjob.application.Models.Entity.Conversation;
import com.webjob.application.Models.Entity.Job;
import com.webjob.application.Models.Entity.Message;
import com.webjob.application.Models.Entity.User;
import com.webjob.application.Models.Request.Search.JobFiltersearch;
import com.webjob.application.Models.Request.Search.MessageFilterRequest;
import com.webjob.application.Models.Request.Websockets.*;
import com.webjob.application.Models.Response.ConversationDTO;
import com.webjob.application.Models.Response.MessagesDTO;
import com.webjob.application.Models.Response.Messensage.MessageResponseDTO;
import com.webjob.application.Models.Response.MetaDTO;
import com.webjob.application.Models.Response.ResponseDTO;
import com.webjob.application.Repository.ConversationRepository;
import com.webjob.application.Repository.MessageRepository;
import com.webjob.application.Repository.UserRepository;
import com.webjob.application.Services.UserService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class MessageService {
    private final MessageRepository messageRepository;
    private final ConversationRepository conversationRepository;
    private final UserRepository userRepository;

    private final UserService userService;
    private final MessageMapper messageMapper;

    private final SimpMessagingTemplate messagingTemplate;

    public MessageService(MessageRepository messageRepository,
                          ConversationRepository conversationRepository,
                          UserRepository userRepository, UserService userService,
                          MessageMapper messageMapper, SimpMessagingTemplate messagingTemplate) {
        this.messageRepository = messageRepository;
        this.conversationRepository = conversationRepository;
        this.userRepository = userRepository;
        this.userService = userService;
        this.messageMapper = messageMapper;
        this.messagingTemplate = messagingTemplate;
    }

    public MessageResponseDTO sendMessage(String senderEmail, MessageRequestDTO requestDTO) {
        User sender = userService.getbyEmail(senderEmail);

        User receiver = userRepository.findById(requestDTO.getReceiverId())
                .orElseThrow(() -> new RuntimeException("Người nhận không tồn tại"));

        Message message = new Message();
        message.setContent(requestDTO.getContent());
        message.setSender(sender);
        message.setReceiver(receiver);
        message.setType(requestDTO.getType());
        message.setStatus(Message.MessageStatus.SENT);

        Message savedMessage = messageRepository.save(message);

        // Cập nhật hoặc tạo conversation
        updateOrCreateConversation(sender, receiver, savedMessage);

        return messageMapper.toResponseDTO(savedMessage);
    }

    public MessageResponseDTO updateMessage(String userEmail, MessageUpdateDTO updateDTO) {
        Message message = messageRepository.findById(updateDTO.getMessageId())
                .orElseThrow(() -> new RuntimeException("Tin nhắn không tồn tại"));
        User user = userService.getbyEmail(userEmail);
        if (message.getSender().getId() != user.getId()) {
            throw new RuntimeException("Bạn chỉ có thể sửa tin nhắn của mình");
        }
        message.setContent(updateDTO.getContent());
        message.setIsEdited(true);
        Message updatedMessage = messageRepository.save(message);

        return messageMapper.toResponseDTO(updatedMessage);
    }

    public void deleteMessage(String userEmail, Long messageId) {
        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new RuntimeException("Tin nhắn không tồn tại"));

        User user = userService.getbyEmail(userEmail);

        if (message.getSender().getId() != user.getId()) {
            throw new RuntimeException("Bạn chỉ có thể sửa tin nhắn của mình");
        }

        message.setIsDeleted(true);
        messageRepository.save(message);

        // Gửi thông báo xóa tin nhắn cho cả sender và receiver
        MessageDeleteDTO deleteDTO = new MessageDeleteDTO(message.getId(), "Tin nhắn đã bị xóa");

        messagingTemplate.convertAndSendToUser(
                message.getSender().getEmail(),
                "/queue/message-deletes",
                deleteDTO
        );

        messagingTemplate.convertAndSendToUser(
                message.getReceiver().getEmail(),
                "/queue/message-deletes",
                deleteDTO
        );
    }

    @Transactional
    public List<MessageResponseDTO> getMessagesBetweenUsers(String userEmail, Long otherUserId) {
        User user = userService.getbyEmail(userEmail);
        List<Message> messages = messageRepository.findMessagesBetweenUsers(user.getId(), otherUserId);

        // Đánh dấu tin nhắn là đã đọc
        messageRepository.markMessagesAsRead(user.getId(), otherUserId);

        return messages.stream()
                .map(messageMapper::toResponseDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public List<ConversationResponseDTO> getUserConversations(String userEmail) {
        User user = userService.getbyEmail(userEmail);

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

    @Transactional(readOnly = true)
    public List<UserInfoDTO> searchUsers(final String userEmail, final String searchTerm) {
        final User currentUser = userService.getbyEmail(userEmail);
        final List<User> users = isHR(currentUser)
                ? userRepository.findCandidatesByName(searchTerm)
                : userRepository.findHRsByName(searchTerm);

        return users.stream()
                .map(messageMapper::toUserInfoDTO)
                .collect(Collectors.toList());
    }

    private boolean isHR(User user) {
        return "HR".equalsIgnoreCase(user.getRole().getName());
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



    public ResponseDTO<?> getPaginated(MessageFilterRequest filterRequest) {
        int page = 0;
        int size = 8;
        try {
            page = Integer.parseInt(filterRequest.getPage());
            if (page <= 0)
                page = 1;
        } catch (NumberFormatException e) {
            // Nếu người dùng nhập sai, mặc định về trang đầu
            page = 1;
        }
        Pageable pageable = PageRequest.of(page-1,filterRequest.getSize(),Sort.by("createdAt").descending());
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
        Long totalItem =messages.getTotalElements();

        MetaDTO metaDTO = new MetaDTO(currentpage, pagesize, totalpage, totalItem);
        List<Message> list =messages.getContent();
        List<MessagesDTO> listmessage=list.stream().map(messageMapper::toDTO).collect(Collectors.toList());
        ResponseDTO<?> respond = new ResponseDTO<>(metaDTO,listmessage);
        return respond;

    }
    public boolean softDeleteMessage(Long id) {
        return messageRepository.findById(id).map(message -> {
            message.setIsDeleted(true);
            messageRepository.save(message);
            return true;
        }).orElse(false);
    }



}
