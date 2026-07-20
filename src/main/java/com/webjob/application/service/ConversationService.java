package com.webjob.application.service;

import com.webjob.application.config.Socket.MessageMapper;
import com.webjob.application.dto.Response.ApiResponse;
import com.webjob.application.models.Entity.Conversation;
import com.webjob.application.dto.Request.Search.ConversationFilter;
import com.webjob.application.dto.Response.ConversationDTO;
import com.webjob.application.dto.Response.MetaDTO;
import com.webjob.application.dto.Response.ResponseDTO;
import com.webjob.application.repository.ConversationRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;


import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ConversationService {
    private final ConversationRepository conversationRepository;
    private final MessageMapper messageMapper;


    public ResponseDTO<List<ConversationDTO>> getPaginated(int page,int size,ConversationFilter filter) {
        if(filter==null){
            filter=new ConversationFilter();
        }

        size = Math.min(Math.max(size, 1), 50);
        page = Math.max(page, 1);
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
        ResponseDTO<List<ConversationDTO>> respond = new ResponseDTO<>(metaDTO,listmessage);
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

    public ResponseDTO<List<ConversationDTO>> getAllConversations(int page, int size,ConversationFilter filter) {
        ResponseDTO<List<ConversationDTO>> respond = getPaginated(page,size,filter);
        return respond;
    }

    public ConversationDTO getConversationById( Long id) {
        Conversation conversation = getbyID(id);
        ConversationDTO conversationDTO = messageMapper.toDTO(conversation);
        return conversationDTO;

    }
    @Transactional
    public void deleteConversationById( Long id) {
        deleteConversation(id);


    }

}
