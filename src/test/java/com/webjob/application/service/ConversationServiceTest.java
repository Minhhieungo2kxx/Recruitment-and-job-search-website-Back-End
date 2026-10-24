package com.webjob.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import com.webjob.application.dto.Request.Search.ConversationFilter;
import com.webjob.application.dto.Response.ConversationDTO;
import com.webjob.application.dto.Response.Messensage.MessageMapper;
import com.webjob.application.dto.Response.ResponseDTO;
import com.webjob.application.models.Entity.Conversation;
import com.webjob.application.repository.ConversationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

@ExtendWith(MockitoExtension.class)
@DisplayName("ConversationService Unit Tests")
public class ConversationServiceTest {
    @Mock
    private ConversationRepository conversationRepository;

    @Mock
    private MessageMapper messageMapper;

    @InjectMocks
    private ConversationService conversationService;

    private Conversation conversation;
    private ConversationDTO conversationDTO;

    @BeforeEach
    void setUp() {
        conversation = mock(Conversation.class);
        conversationDTO = mock(ConversationDTO.class);
    }

    @Nested
    @DisplayName("getPaginated")
    class GetPaginatedTests {

        @Test
        @DisplayName("Should filter conversations by user ID")
        void shouldFilterByUserId() {
            Long userId = 10L;
            ConversationFilter filter = new ConversationFilter();
            filter.setUserId(userId);

            Page<Conversation> page = new PageImpl<>(
                    List.of(conversation),
                    PageRequest.of(0, 10),
                    1
            );

            when(conversationRepository.findAllByUser1_IdOrUser2_Id(
                    eq(userId), eq(userId), any(Pageable.class)
            )).thenReturn(page);

            when(messageMapper.toDTO(conversation))
                    .thenReturn(conversationDTO);

            ResponseDTO<List<ConversationDTO>> result =
                    conversationService.getPaginated(1, 10, filter);

            assertThat(result).isNotNull();
            assertThat(result.getResult()).containsExactly(conversationDTO);

            verify(conversationRepository).findAllByUser1_IdOrUser2_Id(
                    eq(userId), eq(userId), argThat(pageable ->
                            pageable.getPageNumber() == 0
                                    && pageable.getPageSize() == 10
                                    && pageable.getSort().getOrderFor("updatedAt")
                                    .getDirection() == Sort.Direction.DESC
                    )
            );

            verify(conversationRepository, never())
                    .findAllByCreatedAtBetween(any(), any(), any(Pageable.class));
            verify(conversationRepository, never())
                    .findAll(any(Pageable.class));
            verify(messageMapper).toDTO(conversation);
        }

        @Test
        @DisplayName("Should filter conversations by date range")
        void shouldFilterByDateRange() {
            LocalDateTime startLocalDateTime = LocalDateTime.of(2025, 1, 1, 0, 0);
            LocalDateTime endLocalDateTime = LocalDateTime.of(2025, 1, 31, 23, 59);

            ZoneId zoneId = ZoneId.of("Asia/Ho_Chi_Minh");

            Instant startDate = startLocalDateTime.atZone(zoneId).toInstant();
            Instant endDate = endLocalDateTime.atZone(zoneId).toInstant();

            ConversationFilter filter = new ConversationFilter();
            filter.setStartDate(startDate);
            filter.setEndDate(endDate);

            Page<Conversation> page = new PageImpl<>(
                    List.of(conversation),
                    PageRequest.of(0, 10),
                    1
            );

            when(conversationRepository.findAllByCreatedAtBetween(
                    eq(startDate), eq(endDate), any(Pageable.class)
            )).thenReturn(page);

            when(messageMapper.toDTO(conversation))
                    .thenReturn(conversationDTO);

            ResponseDTO<List<ConversationDTO>> result =
                    conversationService.getPaginated(1, 10, filter);

            assertThat(result.getResult()).containsExactly(conversationDTO);

            verify(conversationRepository).findAllByCreatedAtBetween(
                    eq(startDate), eq(endDate), any(Pageable.class)
            );
            verify(conversationRepository, never())
                    .findAll(any(Pageable.class));
        }

        @Test
        @DisplayName("Should return all conversations when filter is null")
        void shouldReturnAllWhenFilterIsNull() {
            Page<Conversation> page = new PageImpl<>(
                    List.of(conversation),
                    PageRequest.of(0, 10),
                    1
            );

            when(conversationRepository.findAll(any(Pageable.class)))
                    .thenReturn(page);
            when(messageMapper.toDTO(conversation))
                    .thenReturn(conversationDTO);

            ResponseDTO<List<ConversationDTO>> result =
                    conversationService.getPaginated(1, 10, null);

            assertThat(result.getResult()).containsExactly(conversationDTO);

            verify(conversationRepository).findAll(
                    ArgumentMatchers.<Pageable>argThat(pageable ->
                            pageable.getPageNumber() == 0
                                    && pageable.getPageSize() == 10
                                    && pageable.getSort().getOrderFor("updatedAt")
                                    .getDirection() == Sort.Direction.DESC
                    ));

            verify(conversationRepository, never())
                    .findAllByCreatedAtBetween(any(), any(), any(Pageable.class));
            verify(conversationRepository, never())
                    .findAllByUser1_IdOrUser2_Id(anyLong(), anyLong(), any(Pageable.class));
        }

        @Test
        @DisplayName("Should prioritize user ID when both filters are provided")
        void shouldPrioritizeUserIdOverDateRange() {
            ConversationFilter filter = new ConversationFilter();
            filter.setUserId(10L);

            LocalDateTime startLocalDateTime = LocalDateTime.of(2025, 1, 1, 0, 0);
            LocalDateTime endLocalDateTime = LocalDateTime.of(2025, 1, 31, 23, 59);

            ZoneId zoneId = ZoneId.of("Asia/Ho_Chi_Minh");

            Instant startDate = startLocalDateTime.atZone(zoneId).toInstant();
            Instant endDate = endLocalDateTime.atZone(zoneId).toInstant();
            filter.setStartDate(startDate);
            filter.setEndDate(endDate);

            Page<Conversation> page = new PageImpl<>(List.of());

            when(conversationRepository.findAllByUser1_IdOrUser2_Id(
                    eq(10L), eq(10L), any(Pageable.class)
            )).thenReturn(page);

            ResponseDTO<List<ConversationDTO>> result =
                    conversationService.getPaginated(1, 10, filter);

            assertThat(result.getResult()).isEmpty();

            verify(conversationRepository).findAllByUser1_IdOrUser2_Id(
                    eq(10L), eq(10L), any(Pageable.class)
            );
            verify(conversationRepository, never())
                    .findAllByCreatedAtBetween(any(), any(), any(Pageable.class));
        }

        @Test
        @DisplayName("Should normalize page and size to valid minimum values")
        void shouldNormalizePageAndSizeToMinimum() {
            Page<Conversation> page = new PageImpl<>(List.of());

            when(conversationRepository.findAll(any(Pageable.class)))
                    .thenReturn(page);

            conversationService.getPaginated(0, 0, null);

            verify(conversationRepository).findAll(
                    ArgumentMatchers.<Pageable>argThat(pageable ->
                            pageable.getPageNumber() == 0
                                    && pageable.getPageSize() == 1

                    ));

        }

        @Test
        @DisplayName("Should limit page size to 50")
        void shouldLimitPageSizeToMaximum() {
            Page<Conversation> page = new PageImpl<>(List.of());

            when(conversationRepository.findAll(any(Pageable.class)))
                    .thenReturn(page);

            conversationService.getPaginated(1, 100, null);

            verify(conversationRepository).findAll(

                    ArgumentMatchers.<Pageable>argThat(pageable ->
                            pageable.getPageNumber() == 0
                                    && pageable.getPageSize() == 50

                    ));

        }

        @Test
        @DisplayName("Should return empty content and correct pagination metadata")
        void shouldReturnEmptyPageWithMetadata() {
            Page<Conversation> page = new PageImpl<>(
                    List.of(),
                    PageRequest.of(1, 10),
                    0
            );

            when(conversationRepository.findAll(any(Pageable.class)))
                    .thenReturn(page);

            ResponseDTO<List<ConversationDTO>> result =
                    conversationService.getPaginated(2, 10, null);

            assertThat(result).isNotNull();
            assertThat(result.getResult()).isEmpty();

            verify(messageMapper, never()).toDTO(any(Conversation.class));
        }


    }


    @Nested
    @DisplayName("getbyID")
    class GetByIdTests {

        @Test
        @DisplayName("Should return conversation when it exists")
        void shouldReturnConversationWhenExists() {
            when(conversationRepository.findById(1L))
                    .thenReturn(Optional.of(conversation));

            Conversation result = conversationService.getbyID(1L);

            assertThat(result).isSameAs(conversation);
            verify(conversationRepository).findById(1L);
        }

    }

    @Test
    @DisplayName("Should throw exception when conversation does not exist")
    void shouldThrowWhenConversationDoesNotExist() {
        when(conversationRepository.findById(99L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> conversationService.getbyID(99L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Conversation not found with 99");

        verify(conversationRepository).findById(99L);
    }

    @Nested
    @DisplayName("getConversationById")
    class GetConversationByIdTests {
        @Test
        @DisplayName("Should return mapped DTO when conversation exists")
        void shouldReturnMappedDTO() {
            when(conversationRepository.findById(1L))
                    .thenReturn(Optional.of(conversation));
            when(messageMapper.toDTO(conversation))
                    .thenReturn(conversationDTO);

            ConversationDTO result =
                    conversationService.getConversationById(1L);

            assertThat(result).isSameAs(conversationDTO);
            verify(messageMapper).toDTO(conversation);
        }
        @Test
        @DisplayName("Should throw exception when conversation does not exist")
        void shouldThrowWhenConversationDoesNotExist() {
            when(conversationRepository.findById(99L))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(
                    () -> conversationService.getConversationById(99L)
            )
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Conversation not found with 99");

            verify(messageMapper, never()).toDTO(any(Conversation.class));
        }




    }

    @Nested
    @DisplayName("deleteConversation")
    class DeleteConversationTests {

        @Test
        @DisplayName("Should delete conversation when it exists")
        void shouldDeleteConversationWhenExists() {
            when(conversationRepository.findById(1L))
                    .thenReturn(Optional.of(conversation));

            conversationService.deleteConversation(1L);

            verify(conversationRepository).delete(conversation);
        }

        @Test
        @DisplayName("Should not delete when conversation does not exist")
        void shouldNotDeleteWhenConversationDoesNotExist() {
            when(conversationRepository.findById(99L))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(
                    () -> conversationService.deleteConversation(99L)
            )
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Conversation not found with 99");

            verify(conversationRepository, never()).delete(any(Conversation.class));
        }
    }

}



