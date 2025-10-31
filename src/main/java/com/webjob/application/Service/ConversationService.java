package com.webjob.application.Service;

import com.webjob.application.Config.Socket.MessageMapper;
import com.webjob.application.Model.Entity.Conversation;
import com.webjob.application.Dto.Request.Search.ConversationFilter;
import com.webjob.application.Dto.Response.ConversationDTO;
import com.webjob.application.Dto.Response.MetaDTO;
import com.webjob.application.Dto.Response.ResponseDTO;
import com.webjob.application.Repository.ConversationRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ConversationService {
    private final ConversationRepository conversationRepository;
    private final MessageMapper messageMapper;

    public ConversationService(ConversationRepository conversationRepository, MessageMapper messageMapper) {
        this.conversationRepository = conversationRepository;
        this.messageMapper = messageMapper;
    }
    public ResponseDTO<?> getPaginated(ConversationFilter filter) {
        int page = 0;
        int size = 8;
        try {
            page = Integer.parseInt(filter.getPage());
            if (page <= 0)
                page = 1;
        } catch (NumberFormatException e) {
            // Nếu người dùng nhập sai, mặc định về trang đầu
            page = 1;
        }
        Pageable pageable = PageRequest.of(page-1, size, Sort.by("updatedAt").descending());
        Page<Conversation> conversations;

        if (filter.getUserId() != null) {
            // Lọc theo user tham gia
            conversations = conversationRepository.findAllByUser1_IdOrUser2_Id(filter.getUserId(),filter.getUserId(), pageable);
        } else if (filter.getStartDate() != null && filter.getEndDate() != null) {
            // Lọc theo khoảng thời gian
            conversations = conversationRepository.findAllByCreatedAtBetween(filter.getStartDate(),filter.getEndDate(), pageable);
        } else {
            // Mặc định trả về tất cả
            conversations = conversationRepository.findAll(pageable);
        }
        int currentpage = conversations.getNumber() + 1;
        int pagesize = conversations.getSize();
        int totalpage = conversations.getTotalPages();
        Long totalItem =conversations.getTotalElements();

        MetaDTO metaDTO = new MetaDTO(currentpage, pagesize, totalpage, totalItem);
        List<Conversation> list =conversations.getContent();
        List<ConversationDTO> listmessage=list.stream().map(messageMapper::toDTO).collect(Collectors.toList());
        ResponseDTO<?> respond = new ResponseDTO<>(metaDTO,listmessage);
        return respond;

    }
    public Conversation getbyID(Long id){
        Conversation conversation=conversationRepository.findById(id)
                .orElseThrow(()->new IllegalArgumentException("Conversation not found with "+id));
        return conversation;
    }

    public void deleteConversation(Long id){
        Conversation conversation=getbyID(id);
        conversationRepository.delete(conversation);

    }

}
