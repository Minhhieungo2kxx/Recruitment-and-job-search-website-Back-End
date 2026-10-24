package com.webjob.application.service;

import com.webjob.application.component.SecurityUtils;
import com.webjob.application.elasticsearch.document.CompanyDocument;
import com.webjob.application.dto.Request.CompanyDTO;
import com.webjob.application.dto.Request.CompanySearchRequest;
import com.webjob.application.dto.Request.OutboxDTO;
import com.webjob.application.dto.Response.CompanyResponse;
import com.webjob.application.dto.Response.ElasticsearchSearchResult;
import com.webjob.application.elasticsearch.company.CompanyElasticsearchSearchService;
import com.webjob.application.enums.CompanyStatus;
import com.webjob.application.enums.OutboxCategory;
import com.webjob.application.enums.OutboxEventType;
import com.webjob.application.exception.Customs.CompanyAlreadyExistsException;
import com.webjob.application.exception.Customs.ResourceNotFoundException;
import com.webjob.application.mapper.CompanyMapper;
import com.webjob.application.messaging.config.RabbitMQConfig;
import com.webjob.application.models.Entity.Company;
import com.webjob.application.models.Entity.Industry;
import com.webjob.application.models.Entity.User;
import com.webjob.application.repository.*;
import com.webjob.application.service.OutBox.OutboxService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CompanyService Unit Tests")
public class CompanyServiceTest {
    @Mock
    private CompanyRepository companyRepository;

    @Mock
    private ModelMapper modelMapper;

    @Mock
    private UserRepository userRepository;

    @Mock
    private SecurityUtils securityUtils;

    @Mock
    private JobRepository jobRepository;

    @Mock
    private FollowCompanyRepository followCompanyRepository;

    @Mock
    private IndustryRepository industryRepository;

    @Mock
    private CompanyElasticsearchSearchService elasticsearchSearchService;

    @Mock
    private CompanyMapper companyMapper;

    @Mock
    private ApplicationEventPublisher publisher;

    @Mock
    private OutboxService outboxService;

    @InjectMocks
    private CompanyService companyService;

    private Company company;
    private Industry industry;

    @BeforeEach
    void setUp() {
        industry = new Industry();
        industry.setId(1L);
        industry.setName("Technology");
        industry.setDeleted(false);

        company = new Company();
        company.setId(10L);
        company.setName("ABC Technology");
        company.setIndustry(industry);
        company.setStatus(CompanyStatus.ACTIVE);
        company.setDeleted(false);
        company.setTaxCode("0123456789");
    }

    @Test
    void createCompany_shouldSaveCompanyAndPublishIndexEvent_whenValidRequest() {
        CompanyDTO request = new CompanyDTO();
        request.setName("ABC Technology");
        request.setIndustryId(1L);
        request.setTaxCode("0123456789");

        Company mappedCompany = new Company();
        mappedCompany.setName("ABC Technology");
        mappedCompany.setTaxCode("0123456789");

        Company savedCompany = new Company();
        savedCompany.setId(10L);
        savedCompany.setName("ABC Technology");
        savedCompany.setIndustry(industry);
        savedCompany.setTaxCode("0123456789");

        when(companyRepository.existsByName("ABC Technology"))
                .thenReturn(false);
        when(modelMapper.map(request, Company.class))
                .thenReturn(mappedCompany);
        when(industryRepository.findByIdAndDeletedFalse(1L))
                .thenReturn(Optional.of(industry));
        when(companyRepository.save(any(Company.class)))
                .thenReturn(savedCompany);
        when(companyMapper.toDocument(savedCompany))
                .thenReturn(CompanyDocument.builder().id(10L).build());

        CompanyResponse response = companyService.createCompany(request);

        assertNotNull(response);
        assertEquals(0, response.getJobCount());
        assertEquals(0, response.getFollowerCount());
        assertEquals("Technology", response.getIndustry());

        ArgumentCaptor<Company> companyCaptor = ArgumentCaptor.forClass(Company.class);

        verify(companyRepository).save(companyCaptor.capture());
        assertEquals(industry, companyCaptor.getValue().getIndustry());

        verify(outboxService).save(any(OutboxDTO.class));
        verify(companyRepository).existsByName("ABC Technology");
    }
    @Test
    void createCompany_shouldThrowException_whenNameAlreadyExists() {
        CompanyDTO request = new CompanyDTO();
        request.setName("ABC Technology");

        when(companyRepository.existsByName("ABC Technology")).thenReturn(true);

        assertThrows(
                CompanyAlreadyExistsException.class,
                () -> companyService.createCompany(request)
        );

        verify(companyRepository, never()).save(any(Company.class));
        verify(outboxService, never()).save(any(OutboxDTO.class));
        verifyNoInteractions(industryRepository);
    }

    @Test
    void createCompany_shouldThrowException_whenIndustryDoesNotExist() {
        CompanyDTO request = new CompanyDTO();
        request.setName("ABC Technology");
        request.setIndustryId(99L);

        when(companyRepository.existsByName("ABC Technology"))
                .thenReturn(false);
        when(modelMapper.map(request, Company.class))
                .thenReturn(new Company());
        when(industryRepository.findByIdAndDeletedFalse(99L)).thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> companyService.createCompany(request)
        );

        verify(companyRepository, never()).save(any(Company.class));
        verify(outboxService, never()).save(any(OutboxDTO.class));
    }



    @Test
    void update_shouldThrowException_whenCompanyDoesNotExist() {
        CompanyDTO request = new CompanyDTO();

        when(companyRepository.findByIdAndDeletedFalse(10L))
                .thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> companyService.update(10L, request)
        );

        verify(companyRepository, never()).save(any(Company.class));
        verifyNoInteractions(outboxService);
    }

    @Test
    void update_shouldThrowException_whenNewIndustryDoesNotExist() {
        CompanyDTO request = new CompanyDTO();
        request.setIndustryId(99L);

        when(companyRepository.findByIdAndDeletedFalse(10L))
                .thenReturn(Optional.of(company));
        when(industryRepository.findByIdAndDeletedFalse(99L))
                .thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> companyService.update(10L, request)
        );

        verify(companyRepository, never()).save(any(Company.class));
        verifyNoInteractions(outboxService);
    }

    @Test
    void update_shouldKeepCurrentIndustry_whenIndustryIdIsNull() {
        CompanyDTO request = new CompanyDTO();
        request.setIndustryId(null);

        when(companyRepository.findByIdAndDeletedFalse(10L))
                .thenReturn(Optional.of(company));
        when(companyRepository.save(company)).thenReturn(company);
        lenient().when(modelMapper.map(company, CompanyResponse.class))
                .thenReturn(new CompanyResponse());
        when(jobRepository.countByCompanyIdAndDeletedFalse(10L))
                .thenReturn(0);
        when(followCompanyRepository.countByCompanyId(10L))
                .thenReturn(0);
        when(companyMapper.toDocument(company))
                .thenReturn(CompanyDocument.builder().id(10L).build());

        Industry originalIndustry = company.getIndustry();

        companyService.update(10L, request);

        assertSame(originalIndustry, company.getIndustry());

        verify(industryRepository, never())
                .findByIdAndDeletedFalse(anyLong());
        verify(companyRepository).save(company);
    }

    @Test
    void getById_shouldReturnCompanyDetails_whenCompanyExistsAndUserFollows() {
        CompanyResponse mappedResponse = new CompanyResponse();

        when(companyRepository.findByIdAndDeletedFalse(10L))
                .thenReturn(Optional.of(company));
        when(modelMapper.map(company, CompanyResponse.class))
                .thenReturn(mappedResponse);
        when(jobRepository.countByCompanyIdAndDeletedFalse(10L))
                .thenReturn(5);
        when(followCompanyRepository.countByCompanyId(10L))
                .thenReturn(8);
        when(securityUtils.isAuthenticated()).thenReturn(true);
        when(securityUtils.getCurrentUserId()).thenReturn(100L);
        when(followCompanyRepository.existsByUserIdAndCompanyId(100L, 10L))
                .thenReturn(true);

        CompanyResponse response = companyService.getById(10L);

        assertSame(mappedResponse, response);
        assertEquals(5, response.getJobCount());
        assertEquals(8, response.getFollowerCount());
        assertTrue(response.isFollowed());

        verify(followCompanyRepository)
                .existsByUserIdAndCompanyId(100L, 10L);
    }

    @Test
    void getById_shouldReturnNotFollowed_whenUserIsNotAuthenticated() {
        CompanyResponse mappedResponse = new CompanyResponse();

        when(companyRepository.findByIdAndDeletedFalse(10L))
                .thenReturn(Optional.of(company));
        when(modelMapper.map(company, CompanyResponse.class))
                .thenReturn(mappedResponse);
        when(jobRepository.countByCompanyIdAndDeletedFalse(10L))
                .thenReturn(0);
        when(followCompanyRepository.countByCompanyId(10L))
                .thenReturn(0);
        when(securityUtils.isAuthenticated()).thenReturn(false);

        CompanyResponse response = companyService.getById(10L);

        assertFalse(response.isFollowed());
        assertEquals(0, response.getJobCount());
        assertEquals(0, response.getFollowerCount());

        verify(followCompanyRepository, never())
                .existsByUserIdAndCompanyId(anyLong(), anyLong());
    }

    @Test
    void getById_shouldThrowException_whenCompanyDoesNotExist() {
        when(companyRepository.findByIdAndDeletedFalse(10L))
                .thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> companyService.getById(10L)
        );

        verifyNoInteractions(jobRepository, followCompanyRepository);
    }

    @Test
    void deleteCompanyById_shouldSoftDeleteCompanyAndPublishEvent() {
        User currentUser = new User();
        currentUser.setEmail("admin@example.com");

        when(companyRepository.findByIdAndDeletedFalse(10L))
                .thenReturn(Optional.of(company));
        when(securityUtils.getCurrentUser()).thenReturn(currentUser);
        when(companyRepository.save(any(Company.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
//        when(companyMapper.toDocument(any(Company.class)))
//                .thenReturn(CompanyDocument.builder().id(10L).build());

        companyService.deleteCompanyById(10L);

        ArgumentCaptor<Company> captor = ArgumentCaptor.forClass(Company.class);

        verify(companyRepository).save(captor.capture());

        Company saved = captor.getValue();

        assertTrue(saved.getDeleted());
        assertNotNull(saved.getDeletedAt());
        assertEquals("admin@example.com", saved.getDeletedBy());
        assertEquals(CompanyStatus.INACTIVE, saved.getStatus());

        verify(outboxService).save(any(OutboxDTO.class));
    }

    @Test
    void deleteCompanyById_shouldUseSystem_whenCurrentUserIsNull() {
        when(companyRepository.findByIdAndDeletedFalse(10L))
                .thenReturn(Optional.of(company));
        when(securityUtils.getCurrentUser()).thenReturn(null);
        when(companyRepository.save(any(Company.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
//        when(companyMapper.toDocument(any(Company.class)))
//                .thenReturn(CompanyDocument.builder().id(10L).build());

        companyService.deleteCompanyById(10L);

        ArgumentCaptor<Company> captor = ArgumentCaptor.forClass(Company.class);

        verify(companyRepository).save(captor.capture());

        assertEquals("SYSTEM", captor.getValue().getDeletedBy());
    }

    @Test
    void deleteCompanyById_shouldThrowException_whenCompanyDoesNotExist() {
        when(companyRepository.findByIdAndDeletedFalse(10L))
                .thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> companyService.deleteCompanyById(10L)
        );

        verify(companyRepository, never()).save(any(Company.class));
        verifyNoInteractions(outboxService);
    }

    @Test
    void restoreCompanyById_shouldRestoreCompanyAndPublishEvent() {
        company.setDeleted(true);
        company.setDeletedAt(Instant.now());
        company.setStatus(CompanyStatus.INACTIVE);

        when(companyRepository.findByIdAndDeletedTrue(10L))
                .thenReturn(Optional.of(company));
        when(companyRepository.save(any(Company.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
//        when(companyMapper.toDocument(any(Company.class)))
//                .thenReturn(CompanyDocument.builder().id(10L).build());

        companyService.restoreCompanyById(10L);

        ArgumentCaptor<Company> captor = ArgumentCaptor.forClass(Company.class);

        verify(companyRepository).save(captor.capture());

        Company restored = captor.getValue();

        assertFalse(restored.getDeleted());
        assertNull(restored.getDeletedAt());
        assertNull(restored.getDeletedBy());
        assertEquals(CompanyStatus.ACTIVE, restored.getStatus());

        verify(outboxService).save(any(OutboxDTO.class));
    }

    @Test
    void restoreCompanyById_shouldThrowException_whenCompanyIsNotDeleted() {
        when(companyRepository.findByIdAndDeletedTrue(10L))
                .thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> companyService.restoreCompanyById(10L)
        );

        verify(companyRepository, never()).save(any(Company.class));
        verifyNoInteractions(outboxService);
    }

    @Test
    void delete_shouldRemoveCompanyReferencesFromUsersBeforeDeleting() {
        User user1 = new User();
        user1.setCompany(company);

        User user2 = new User();
        user2.setCompany(company);

        when(userRepository.findAllByCompanyAndDeletedFalse(company))
                .thenReturn(List.of(user1, user2));

        companyService.delete(company);

        assertNull(user1.getCompany());
        assertNull(user2.getCompany());

        verify(userRepository).saveAll(List.of(user1, user2));
        verify(companyRepository).delete(company);
    }

    @Test
    void delete_shouldDeleteCompany_whenNoUsersAreAssociated() {
        when(userRepository.findAllByCompanyAndDeletedFalse(company))
                .thenReturn(Collections.emptyList());

        companyService.delete(company);

        verify(userRepository).saveAll(Collections.emptyList());
        verify(companyRepository).delete(company);
    }

    @Test
    void searchCompaniesClient_shouldFallbackToDatabase_whenElasticsearchFails() {
        CompanySearchRequest request = new CompanySearchRequest();
        request.setKeyword("ABC");

        Page<Company> expectedPage = new PageImpl<>(
                List.of(company),
                PageRequest.of(0, 10, Sort.by(Sort.Direction.ASC, "name")),
                1
        );

        when(elasticsearchSearchService.searchCompanyClient(0, 10, request))
                .thenThrow(new RuntimeException("Elasticsearch unavailable"));

        when(companyRepository.findAll(
                ArgumentMatchers.<Specification<Company>>any(),
                ArgumentMatchers.<Pageable>any()
        )).thenReturn(expectedPage);

        Page<Company> result =
                companyService.searchCompaniesClient(0, 10, request);

        assertEquals(1, result.getTotalElements());
        assertEquals("ABC Technology", result.getContent().get(0).getName());

        verify(companyRepository).findAll(
                ArgumentMatchers.<Specification<Company>>any(),
                ArgumentMatchers.<Pageable>argThat(pageable ->
                        pageable != null
                                && pageable.getPageNumber() == 0
                                && pageable.getPageSize() == 10
                                && pageable.getSort().getOrderFor("name") != null
                ));
    }

    @Test
    void searchCompaniesClient_shouldReturnEmptyPage_whenElasticsearchReturnsNoIds() {
        CompanySearchRequest request = new CompanySearchRequest();
        List<Long> ids=new ArrayList<>();

        ElasticsearchSearchResult searchResult = new ElasticsearchSearchResult(ids,0L);

        // Thiết lập searchResult có danh sách IDs rỗng và total = 0
        // theo constructor/setter thực tế của dự án.

        when(elasticsearchSearchService.searchCompanyClient(0, 10, request)).thenReturn(searchResult);

        Page<Company> result =
                companyService.searchCompaniesClient(0, 10, request);

        assertTrue(result.isEmpty());
        assertEquals(0, result.getTotalElements());

        verify(companyRepository, never()).findByIdIn(anyList());
        verify(companyRepository, never())
                .findAll(any(Specification.class), any(Pageable.class));
    }

    @Test
    void searchCompaniesClient_shouldPreserveElasticsearchOrder() {
        Company company10 = new Company();
        company10.setId(10L);

        Company company20 = new Company();
        company20.setId(20L);

        CompanySearchRequest request = new CompanySearchRequest();

        List<Long> ids= Arrays.asList(20L,10L);
        ElasticsearchSearchResult searchResult = new ElasticsearchSearchResult(ids,2L);

        // Thiết lập searchResult.ids = [20L, 10L]
        // và total = 2 theo API thực tế của class.

        when(elasticsearchSearchService.searchCompanyClient(0, 10, request))
                .thenReturn(searchResult);
        when(companyRepository.findByIdIn(anyList()))
                .thenReturn(List.of(company10, company20));

        Page<Company> result =
                companyService.searchCompaniesClient(0, 10, request);

        assertEquals(
                List.of(20L, 10L),
                result.getContent().stream()
                        .map(Company::getId)
                        .toList()
        );
    }

    @Test
    void publishCompanyEventIndex_shouldCreateCompanyCreatedOutboxEvent() {
        when(companyMapper.toDocument(company))
                .thenReturn(CompanyDocument.builder().id(10L).build());

        companyService.publishCompanyEventIndex(company);

        ArgumentCaptor<OutboxDTO> captor = ArgumentCaptor.forClass(OutboxDTO.class);

        verify(outboxService).save(captor.capture());

        OutboxDTO event = captor.getValue();

        assertEquals("COMPANY", event.getAggregateType());
        assertEquals("10", event.getAggregateId());
        assertEquals(OutboxCategory.COMPANY_INDEX, event.getCategory());
        assertEquals(
                OutboxEventType.COMPANY_INDEX_CREATED,
                event.getEventType()
        );
        assertEquals(
                RabbitMQConfig.COMPANY_INDEX_EXCHANGE,
                event.getExchangeName()
        );
        assertEquals(
                RabbitMQConfig.COMPANY_INDEX_CREATED_ROUTING_KEY,
                event.getRoutingKey()
        );
        assertNotNull(event.getPayload());
    }


}
