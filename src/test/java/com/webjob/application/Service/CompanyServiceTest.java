package com.webjob.application.Service;

import com.webjob.application.Model.Entity.*;

import com.webjob.application.Dto.Request.Search.SearchCompanyDTO;
import com.webjob.application.Dto.Response.MetaDTO;
import com.webjob.application.Dto.Response.ResponseDTO;
import com.webjob.application.Repository.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;

import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
@ExtendWith(MockitoExtension.class)
public class CompanyServiceTest {
    @Mock
    private CompanyRepository companyRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ModelMapper modelMapper;

    @InjectMocks
    private CompanyService companyService;


    @Test
    void testHandle_SaveCompany_Success() {
        Company company = new Company();
        company.setName("Test Company");

        when(companyRepository.save(company)).thenReturn(company);

        Company savedCompany = companyService.handle(company);

        assertThat(savedCompany).isEqualTo(company);
        verify(companyRepository, times(1)).save(company);

    }
    @Test
    void testGetAllCompanies() {
        List<Company> companies = List.of(new Company(), new Company());
        when(companyRepository.findAll()).thenReturn(companies);

        List<Company> result = companyService.getAll();

        assertThat(result).hasSize(2);
        verify(companyRepository).findAll();
    }
    @Test
    void testGetCompanyById() {
        Company company = new Company();
        company.setId(1L);

        when(companyRepository.findById(1L)).thenReturn(Optional.of(company));

        Optional<Company> result = companyService.getbyID(1L);

        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(1L);
        verify(companyRepository).findById(1L);
    }
    @Test
    void testCheckByID_WhenExists() {
        when(companyRepository.existsById(1L)).thenReturn(true);

        boolean result = companyService.checkByID(1L);

        assertThat(result).isTrue();
        verify(companyRepository).existsById(1L);
    }

    @Test
    void testCheckByID_WhenNotExists_ThrowsException() {
        when(companyRepository.existsById(1L)).thenReturn(false);

        assertThatThrownBy(() -> companyService.checkByID(1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Không tồn tại Company với ID: 1");
    }
    @Test
    void testDeleteCompany() {
        Company company = new Company();
        company.setId(1L);

        User user1 = new User();
        user1.setCompany(company);

        User user2 = new User();
        user2.setCompany(company);

        List<User> users = List.of(user1, user2);

        when(userRepository.findAllByCompany(company)).thenReturn(users);

        companyService.delete(company);

        assertThat(user1.getCompany()).isNull();
        assertThat(user2.getCompany()).isNull();

        verify(userRepository).saveAll(users);
        verify(companyRepository).delete(company);
    }
    @Test
    void testGetAllPage() {
        int page = 1;
        int limit = 2;

        Page<Company> mockPage = new PageImpl<>(List.of(new Company(), new Company()));
        Pageable pageable = PageRequest.of(page - 1, limit);

        when(companyRepository.findAll(pageable)).thenReturn(mockPage);

        Page<Company> result = companyService.getallPage(page, limit);

        assertThat(result.getContent()).hasSize(2);
        verify(companyRepository).findAll(pageable);
    }
    @Test
    void testGetPaginated_WithSearchType_ReturnsCorrectMetaAndData() {
        // Arrange
        SearchCompanyDTO searchDTO = new SearchCompanyDTO();
        searchDTO.setName("Test");
        searchDTO.setLimit(5);
        searchDTO.setSortBy("name");
        searchDTO.setSortOrder("asc");

        int pageRequested = 3; // Người dùng nhập "3"
        int zeroBasedPage = pageRequested - 1; // JPA Pageable bắt đầu từ 0

        List<Company> companyList = List.of(new Company(), new Company(), new Company());
        Page<Company> mockPage = new PageImpl<>(
                companyList,
                PageRequest.of(zeroBasedPage, searchDTO.getLimit(), Sort.by("name").ascending()),
                15 // Tổng 15 bản ghi -> totalPages = 3
        );

        when(companyRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(mockPage);

        // Act
        ResponseDTO<?> response = companyService.getPaginated(String.valueOf(pageRequested), "search", searchDTO);

        // Assert
        assertThat(response).isNotNull();


        MetaDTO meta = response.getMeta();
        assertThat(meta.getCurrent()).isEqualTo(pageRequested);
        assertThat(meta.getPageSize()).isEqualTo(searchDTO.getLimit());
        assertThat(meta.getTotal()).isEqualTo(15);
        assertThat(meta.getPages()).isEqualTo(3);

        verify(companyRepository, times(1)).findAll(any(Specification.class), any(Pageable.class));
    }







}
