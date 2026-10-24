package com.webjob.application.service;

import com.webjob.application.component.SecurityUtils;
import com.webjob.application.dto.Request.IndustryFilter;
import com.webjob.application.dto.Request.IndustryRequest;
import com.webjob.application.dto.Response.IndustryResponse;
import com.webjob.application.dto.Response.ResponseDTO;
import com.webjob.application.exception.Customs.BadRequestException;
import com.webjob.application.exception.Customs.ResourceNotFoundException;
import com.webjob.application.models.Entity.Industry;
import com.webjob.application.models.Entity.User;
import com.webjob.application.repository.IndustryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("IndustryService Unit Tests")
public class IndustryServiceTest {
    @Mock
    private IndustryRepository industryRepository;

    @Mock
    private ModelMapper modelMapper;

    @Mock
    private SecurityUtils securityUtils;

    @InjectMocks
    private IndustryService industryService;

    private Industry industry;
    private IndustryResponse industryResponse;

    @BeforeEach
    void setUp() {
        industry = new Industry();
        industry.setId(1L);
        industry.setName("Technology");
        industry.setActive(true);
        industry.setDeleted(false);

        industryResponse = new IndustryResponse();

        // Giả lập save() lưu thành công và trả về entity.
        lenient().when(industryRepository.save(any(Industry.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        lenient().when(modelMapper.map(any(Industry.class), eq(IndustryResponse.class)))
                .thenReturn(industryResponse);
    }

    // ==================== CREATE ====================

    @Test
    void create_shouldReturnResponse_whenNameDoesNotExist() {
        IndustryRequest request = new IndustryRequest();
        request.setName("Technology");

        when(industryRepository
                .existsByNameIgnoreCaseAndDeletedFalse("Technology"))
                .thenReturn(false);

        when(modelMapper.map(request, Industry.class))
                .thenReturn(industry);

        IndustryResponse result = industryService.create(request);

        assertThat(result).isSameAs(industryResponse);

        verify(industryRepository)
                .existsByNameIgnoreCaseAndDeletedFalse("Technology");
        verify(industryRepository).save(industry);
        verify(modelMapper).map(industry, IndustryResponse.class);
    }

    @Test
    void create_shouldThrowBadRequest_whenNameAlreadyExists() {
        IndustryRequest request = new IndustryRequest();
        request.setName("Technology");

        when(industryRepository
                .existsByNameIgnoreCaseAndDeletedFalse("Technology"))
                .thenReturn(true);

        BadRequestException exception = assertThrows(
                BadRequestException.class,
                () -> industryService.create(request)
        );

        assertThat(exception.getMessage())
                .contains("Technology")
                .contains("đã tồn tại");

        verify(industryRepository, never()).save(any());
        verify(modelMapper, never()).map(request, Industry.class);
    }
    // ==================== UPDATE ====================

    @Test
    void update_shouldUpdateIndustry_whenRequestIsValid() {
        IndustryRequest request = new IndustryRequest();
        request.setName("Finance");
        request.setActive(false);

        when(industryRepository.findByIdAndDeletedFalse(1L))
                .thenReturn(Optional.of(industry));

        when(industryRepository
                .findByNameIgnoreCaseAndDeletedFalse("Finance"))
                .thenReturn(Optional.empty());

        IndustryResponse result = industryService.update(1L, request);

        assertAll(
                () -> assertThat(result).isSameAs(industryResponse),
                () -> assertEquals("Finance", industry.getName()),
                () -> assertEquals(false, industry.getActive())
        );

        verify(industryRepository).save(industry);
    }

    @Test
    void update_shouldThrowNotFound_whenIndustryDoesNotExist() {
        IndustryRequest request = new IndustryRequest();
        request.setName("Finance");

        when(industryRepository.findByIdAndDeletedFalse(99L))
                .thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> industryService.update(99L, request)
        );

        verify(industryRepository, never())
                .findByNameIgnoreCaseAndDeletedFalse(anyString());
        verify(industryRepository, never()).save(any());
    }

    @Test
    void update_shouldThrowBadRequest_whenNameBelongsToAnotherIndustry() {
        Industry anotherIndustry = new Industry();
        anotherIndustry.setId(2L);
        anotherIndustry.setName("Finance");

        IndustryRequest request = new IndustryRequest();
        request.setName("Finance");
        request.setActive(true);

        when(industryRepository.findByIdAndDeletedFalse(1L))
                .thenReturn(Optional.of(industry));

        when(industryRepository
                .findByNameIgnoreCaseAndDeletedFalse("Finance"))
                .thenReturn(Optional.of(anotherIndustry));

        assertThrows(
                BadRequestException.class,
                () -> industryService.update(1L, request)
        );

        assertEquals("Technology", industry.getName());
        verify(industryRepository, never()).save(any());
    }

    @Test
    void update_shouldAllowSameName_whenUpdatingCurrentIndustry() {
        IndustryRequest request = new IndustryRequest();
        request.setName("Technology");
        request.setActive(true);

        when(industryRepository.findByIdAndDeletedFalse(1L))
                .thenReturn(Optional.of(industry));

        when(industryRepository
                .findByNameIgnoreCaseAndDeletedFalse("Technology"))
                .thenReturn(Optional.of(industry));

        IndustryResponse result = industryService.update(1L, request);

        assertThat(result).isSameAs(industryResponse);
        verify(industryRepository).save(industry);
    }

    // ==================== GET BY ID ====================

    @Test
    void getById_shouldReturnResponse_whenIndustryExists() {
        when(industryRepository.findByIdAndDeletedFalse(1L))
                .thenReturn(Optional.of(industry));

        IndustryResponse result = industryService.getById(1L);

        assertThat(result).isSameAs(industryResponse);
        verify(modelMapper).map(industry, IndustryResponse.class);
    }

    @Test
    void getById_shouldThrowNotFound_whenIndustryDoesNotExist() {
        when(industryRepository.findByIdAndDeletedFalse(99L))
                .thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> industryService.getById(99L)
        );

        verify(modelMapper, never())
                .map(any(Industry.class), eq(IndustryResponse.class));
    }
    // ==================== DELETE ====================

    @Test
    void delete_shouldSoftDeleteIndustry_andRecordAuditInformation() {
        User user = new User();
        user.setEmail("admin@example.com");

        when(industryRepository.findByIdAndDeletedFalse(1L))
                .thenReturn(Optional.of(industry));
        when(securityUtils.getCurrentUser()).thenReturn(user);

        Instant beforeDelete = Instant.now();

        industryService.delete(1L);

        Instant afterDelete = Instant.now();

        assertAll(
                () -> assertThat(industry.getDeleted()).isTrue(),
                () -> assertThat(industry.getDeletedAt())
                        .isBetween(beforeDelete, afterDelete),
                () -> assertEquals(
                        "admin@example.com", industry.getDeletedBy())
        );

        verify(industryRepository).save(industry);
    }

    @Test
    void delete_shouldThrowNotFound_whenIndustryDoesNotExist() {
        when(industryRepository.findByIdAndDeletedFalse(99L))
                .thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> industryService.delete(99L)
        );

        verify(securityUtils, never()).getCurrentUser();
        verify(industryRepository, never()).save(any());
    }

    // ==================== GET ALL ====================

    @Test
    void getAll_shouldReturnMappedResponses() {
        Industry secondIndustry = new Industry();
        secondIndustry.setId(2L);
        secondIndustry.setName("Finance");

        when(industryRepository.findAllByDeletedFalseOrderByNameAsc())
                .thenReturn(List.of(industry, secondIndustry));

        IndustryResponse secondResponse = new IndustryResponse();

        when(modelMapper.map(secondIndustry, IndustryResponse.class))
                .thenReturn(secondResponse);

        List<IndustryResponse> result = industryService.getAll();

        assertThat(result).containsExactly(industryResponse, secondResponse);

        verify(industryRepository).findAllByDeletedFalseOrderByNameAsc();
    }

    @Test
    void getAll_shouldReturnEmptyList_whenNoIndustriesExist() {
        when(industryRepository.findAllByDeletedFalseOrderByNameAsc())
                .thenReturn(List.of());

        List<IndustryResponse> result = industryService.getAll();

        assertThat(result).isEmpty();
        verifyNoInteractions(modelMapper);
    }

    // ==================== RESTORE ====================

    @Test
    void restore_shouldClearDeletionInformation() {
        industry.setDeleted(true);
        industry.setDeletedAt(Instant.now());
        industry.setDeletedBy("admin@example.com");

        when(industryRepository.findByIdAndDeletedTrue(1L))
                .thenReturn(Optional.of(industry));

        IndustryResponse result = industryService.restore(1L);

        assertAll(
                () -> assertThat(result).isSameAs(industryResponse),
                () -> assertThat(industry.getDeleted()).isFalse(),
                () -> assertThat(industry.getDeletedAt()).isNull(),
                () -> assertThat(industry.getDeletedBy()).isNull()
        );

        verify(industryRepository, atLeastOnce()).save(industry);
    }

    @Test
    void restore_shouldThrowNotFound_whenDeletedIndustryDoesNotExist() {
        when(industryRepository.findByIdAndDeletedTrue(99L))
                .thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> industryService.restore(99L)
        );

        verify(industryRepository, never()).save(any());
    }

    // ==================== PAGINATION ====================

    @Test
    void getIndustries_shouldNormalizeInvalidPageAndSize() {
        IndustryFilter filter = new IndustryFilter();

        Page<Industry> pageResult = new PageImpl<>(
                List.of(industry),
                PageRequest.of(0, 10),
                1
        );

        when(industryRepository.findAll(
                any(Specification.class), any(Pageable.class)))
                .thenReturn(pageResult);

        ResponseDTO<List<IndustryResponse>> result =
                industryService.getIndustries(0, -5, filter);

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);

        verify(industryRepository).findAll(any(Specification.class), captor.capture());

        Pageable pageable = captor.getValue();

        assertAll(
                () -> assertEquals(0, pageable.getPageNumber()),
                () -> assertEquals(10, pageable.getPageSize()),
                () -> assertEquals(
                        Sort.Direction.ASC,
                        pageable.getSort().getOrderFor("name").getDirection()
                ),
                () -> assertNotNull(result)
        );
    }

    @Test
    void getIndustries_shouldUseRequestedPageAndSize() {
        IndustryFilter filter = new IndustryFilter();

        when(industryRepository.findAll(
                any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(
                        List.of(industry),
                        PageRequest.of(1, 5),
                        6
                ));

        industryService.getIndustries(2, 5, filter);

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);

        verify(industryRepository).findAll(
                any(Specification.class), captor.capture());

        assertEquals(1, captor.getValue().getPageNumber());
        assertEquals(5, captor.getValue().getPageSize());
    }

    @Test
    void getIndustries_shouldAcceptNullFilter() {
        when(industryRepository.findAll(
                any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        assertNotNull(industryService.getIndustries(1, 10, null));

        verify(industryRepository).findAll(
                any(Specification.class), any(Pageable.class));
    }
    // ==================== ADMIN QUERY ====================

    @Test
    void getIndustriesForAdmin_shouldApplyPaginationAndSorting() {
        IndustryFilter filter = new IndustryFilter();

        when(industryRepository.findAll(
                any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(industry)));

        Page<Industry> result =
                industryService.getIndustriesForAdmin(0, 10, filter);

        ArgumentCaptor<Pageable> captor =
                ArgumentCaptor.forClass(Pageable.class);

        verify(industryRepository).findAll(
                any(Specification.class), captor.capture());

        assertAll(
                () -> assertThat(result.getContent()).containsExactly(industry),
                () -> assertEquals(0, captor.getValue().getPageNumber()),
                () -> assertEquals(10, captor.getValue().getPageSize()),
                () -> assertEquals(
                        Sort.Direction.ASC,
                        captor.getValue().getSort()
                                .getOrderFor("name").getDirection()
                )
        );
    }

    // ==================== RESPONSE CONVERSION ====================

    @Test
    void convertToIndustryResponseDTO_shouldMapPageContent() {
        Page<Industry> page = new PageImpl<>(
                List.of(industry),
                PageRequest.of(1, 10),
                21
        );

        ResponseDTO<List<IndustryResponse>> result =
                industryService.convertToIndustryResponseDTO(page);

        assertNotNull(result);
        verify(modelMapper).map(industry, IndustryResponse.class);

    }



}
