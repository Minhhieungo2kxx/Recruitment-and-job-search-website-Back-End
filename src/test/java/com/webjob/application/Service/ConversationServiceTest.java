package com.webjob.application.service;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

import com.webjob.application.Config.Socket.MessageMapper;
import com.webjob.application.Model.Entity.Conversation;
import com.webjob.application.Dto.Request.Search.ConversationFilter;
import com.webjob.application.Dto.Response.ConversationDTO;
import com.webjob.application.Dto.Response.ResponseDTO;
import com.webjob.application.Repository.ConversationRepository;
import com.webjob.application.Service.ConversationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.springframework.data.domain.*;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.*;

@ExtendWith(org.mockito.junit.jupiter.MockitoExtension.class)
public class ConversationServiceTest {

    @Mock
    private ConversationRepository conversationRepository;

    @Mock
    private MessageMapper messageMapper;

    @InjectMocks
    private ConversationService conversationService;

    // Test cho phương thức getPaginated
    @Test
    void testGetPaginated_withValidPage() {
        // Setup mock filter và data trả về
        ConversationFilter filter = new ConversationFilter();
        filter.setPage("1");

        Conversation conversation1 = new Conversation();
        conversation1.setId(1L);
        Conversation conversation2 = new Conversation();
        conversation2.setId(2L);

        Page<Conversation> page = new PageImpl<>(Arrays.asList(conversation1, conversation2));

        when(conversationRepository.findAll(any(Pageable.class))).thenReturn(page);
        when(messageMapper.toDTO(any(Conversation.class))).thenReturn(new ConversationDTO());

        // Gọi phương thức cần kiểm tra
        ResponseDTO<?> response = conversationService.getPaginated(filter);

        // Kiểm tra kết quả
        assertNotNull(response);
        assertEquals(2, ((List<?>) response.getResult()).size());
        verify(conversationRepository).findAll(any(Pageable.class));
    }
    @Test
    void testGetPaginated_withFilterByDateRange() {
        // Setup filter với khoảng thời gian
        ConversationFilter filter = new ConversationFilter();
        filter.setStartDate(LocalDate.parse("2023-01-01").atStartOfDay(ZoneOffset.UTC).toInstant());
        filter.setEndDate(LocalDate.parse("2023-12-31").atStartOfDay(ZoneOffset.UTC).toInstant());

        Conversation conversation1 = new Conversation();
        conversation1.setId(1L);

        Page<Conversation> page = new PageImpl<>(Collections.singletonList(conversation1));

        when(conversationRepository.findAllByCreatedAtBetween(any(), any(), any(Pageable.class))).thenReturn(page);
        when(messageMapper.toDTO(any(Conversation.class))).thenReturn(new ConversationDTO());

        // Gọi phương thức cần kiểm tra
        ResponseDTO<?> response = conversationService.getPaginated(filter);

        // Kiểm tra kết quả
        assertNotNull(response);
        assertEquals(1, ((List<?>) response.getResult()).size());
        verify(conversationRepository).findAllByCreatedAtBetween(any(), any(), any(Pageable.class));
    }
    @Test
    void testGetPaginated_withFilterByUserId() {
        // Setup filter theo userId
        ConversationFilter filter = new ConversationFilter();
        filter.setUserId(1L);

        Conversation conversation1 = new Conversation();
        conversation1.setId(1L);

        Conversation conversation2 = new Conversation();
        conversation2.setId(2L);

        Page<Conversation> page = new PageImpl<>(Arrays.asList(conversation2,conversation1));

        when(conversationRepository.findAllByUser1_IdOrUser2_Id(eq(1L), eq(1L), any(Pageable.class))).thenReturn(page);
        when(messageMapper.toDTO(any(Conversation.class))).thenReturn(new ConversationDTO());

        // Gọi phương thức cần kiểm tra
        ResponseDTO<?> response = conversationService.getPaginated(filter);

        // Kiểm tra kết quả
        assertNotNull(response);
        assertEquals(2, ((List<?>) response.getResult()).size());
        verify(conversationRepository).findAllByUser1_IdOrUser2_Id(eq(1L), eq(1L), any(Pageable.class));
    }
    // Test cho phương thức getbyID
    @Test
    void testGetbyID_ConversationFound() {
        Conversation conversation = new Conversation();
        conversation.setId(1L);

        when(conversationRepository.findById(1L)).thenReturn(Optional.of(conversation));

        // Gọi phương thức cần kiểm tra
        Conversation result = conversationService.getbyID(1L);

        // Kiểm tra kết quả
        assertNotNull(result);
        assertEquals(1L, result.getId());
        verify(conversationRepository).findById(1L);
    }

    @Test
    void testGetbyID_ConversationNotFound() {
        when(conversationRepository.findById(1L)).thenReturn(Optional.empty());

        // Kiểm tra khi ném exception
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            conversationService.getbyID(1L);
        });

        assertEquals("Conversation not found with 1", exception.getMessage());
        verify(conversationRepository).findById(1L);
    }

    // Test cho phương thức deleteConversation
    @Test
    void testDeleteConversation_ConversationFound() {
        Conversation conversation = new Conversation();
        conversation.setId(1L);

        when(conversationRepository.findById(1L)).thenReturn(Optional.of(conversation));

        // Gọi phương thức cần kiểm tra
        conversationService.deleteConversation(1L);

        // Kiểm tra xem repository đã xóa chưa
        verify(conversationRepository).delete(conversation);
    }

    @Test
    void testDeleteConversation_ConversationNotFound() {
        when(conversationRepository.findById(1L)).thenReturn(Optional.empty());

        // Kiểm tra khi ném exception
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            conversationService.deleteConversation(1L);
        });

        assertEquals("Conversation not found with 1", exception.getMessage());
        verify(conversationRepository, never()).delete(any(Conversation.class));
    }








}
