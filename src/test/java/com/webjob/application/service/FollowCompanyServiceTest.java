package com.webjob.application.service;

import com.webjob.application.component.SecurityUtils;
import com.webjob.application.dto.Response.FollowCompanyResponse;
import com.webjob.application.dto.Response.ResponseDTO;
import com.webjob.application.exception.Customs.BadRequestException;
import com.webjob.application.exception.Customs.ResourceNotFoundException;
import com.webjob.application.models.Entity.Company;
import com.webjob.application.models.Entity.FollowCompany;
import com.webjob.application.models.Entity.User;
import com.webjob.application.repository.CompanyRepository;
import com.webjob.application.repository.FollowCompanyRepository;
import com.webjob.application.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("FollowCompanyService Unit Tests")
public class FollowCompanyServiceTest {
    @Mock
    private FollowCompanyRepository followRepository;

    @Mock
    private CompanyRepository companyRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private SecurityUtils securityUtils;

    @InjectMocks
    private FollowCompanyService followCompanyService;

    private User user;
    private Company company;

    private static final Long USER_ID = 1L;
    private static final Long COMPANY_ID = 10L;
    private static final Long FOLLOW_ID = 100L;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(USER_ID);

        company = new Company();
        company.setId(COMPANY_ID);
        company.setName("OpenAI");
        company.setLogo("openai.png");

        lenient().when(securityUtils.getCurrentUserId()).thenReturn(USER_ID);
    }

    // ==================== followCompany ====================

    @Test
    void followCompany_shouldSaveFollow_whenNotFollowedYet() {
        when(securityUtils.getCurrentUser()).thenReturn(user);
        when(followRepository.existsByUserIdAndCompanyId(
                USER_ID, COMPANY_ID
        )).thenReturn(false);

        when(companyRepository.findByIdAndDeletedFalse(COMPANY_ID))
                .thenReturn(Optional.of(company));

        followCompanyService.followCompany(COMPANY_ID);

        ArgumentCaptor<FollowCompany> captor = ArgumentCaptor.forClass(FollowCompany.class);

        verify(followRepository).save(captor.capture());

        FollowCompany saved = captor.getValue();

        assertThat(saved.getUser()).isSameAs(user);
        assertThat(saved.getCompany()).isSameAs(company);
        assertThat(saved.isNotificationEnabled()).isTrue();

        verify(companyRepository).findByIdAndDeletedFalse(COMPANY_ID);
    }

    @Test
    void followCompany_shouldThrow_whenAlreadyFollowed() {
        when(securityUtils.getCurrentUser()).thenReturn(user);
        when(followRepository.existsByUserIdAndCompanyId(
                USER_ID, COMPANY_ID
        )).thenReturn(true);

        assertThatThrownBy(
                () -> followCompanyService.followCompany(COMPANY_ID)
        )
                .isInstanceOf(BadRequestException.class)
                .hasMessage("FollowCompany already saved.");

        verify(followRepository, never()).save(any());
        verify(companyRepository, never())
                .findByIdAndDeletedFalse(any());
    }

    @Test
    void followCompany_shouldThrow_whenCompanyNotFound() {
        when(securityUtils.getCurrentUser()).thenReturn(user);
        when(followRepository.existsByUserIdAndCompanyId(
                USER_ID, COMPANY_ID
        )).thenReturn(false);

        when(companyRepository.findByIdAndDeletedFalse(COMPANY_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(
                () -> followCompanyService.followCompany(COMPANY_ID)
        )
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Company not found");

        verify(followRepository, never()).save(any());
    }

    // ==================== unfollowCompany ====================

    @Test
    void unfollowCompany_shouldDeleteFollow_whenFound() {
        FollowCompany follow = new FollowCompany();
        follow.setId(FOLLOW_ID);
        follow.setUser(user);
        follow.setCompany(company);

        when(followRepository.findByUserIdAndCompanyId(
                USER_ID, COMPANY_ID
        )).thenReturn(Optional.of(follow));

        followCompanyService.unfollowCompany(COMPANY_ID);

        verify(followRepository).delete(follow);
    }

    @Test
    void unfollowCompany_shouldThrow_whenFollowNotFound() {
        when(followRepository.findByUserIdAndCompanyId(
                USER_ID, COMPANY_ID
        )).thenReturn(Optional.empty());

        assertThatThrownBy(
                () -> followCompanyService.unfollowCompany(COMPANY_ID)
        )
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("FollowCompany not found");

        verify(followRepository, never()).delete(any());
    }

    // ==================== getMyFollowCompanies ====================

    @Test
    void getMyFollowCompanies_shouldReturnPaginatedData() {
        FollowCompany follow = new FollowCompany();
        follow.setId(FOLLOW_ID);
        follow.setUser(user);
        follow.setCompany(company);
        follow.setNotificationEnabled(true);

        Pageable expectedPageable = PageRequest.of(
                0, 10, Sort.by("followedAt").descending()
        );

        Page<FollowCompany> page = new PageImpl<>(
                List.of(follow),
                expectedPageable,
                1
        );

        when(followRepository.findByUserId(
                USER_ID, expectedPageable
        )).thenReturn(page);

        ResponseDTO<List<FollowCompanyResponse>> response =
                followCompanyService.getMyFollowCompanies(1, 10);

        assertThat(response).isNotNull();

        // Nếu DTO sử dụng tên getter khác, điều chỉnh theo class thực tế.
        assertThat(response.getResult()).hasSize(1);

        FollowCompanyResponse item = response.getResult().get(0);

        assertThat(item.getCompanyId()).isEqualTo(COMPANY_ID);
        assertThat(item.getCompanyName()).isEqualTo("OpenAI");
        assertThat(item.getLogo()).isEqualTo("openai.png");
        assertThat(item.isNotificationEnabled()).isTrue();

        verify(followRepository).findByUserId(
                USER_ID, expectedPageable
        );
    }

    @Test
    void getMyFollowCompanies_shouldNormalizeInvalidPagination() {
        Page<FollowCompany> emptyPage = new PageImpl<>(
                List.of(),
                PageRequest.of(
                        0, 10, Sort.by("followedAt").descending()
                ),
                0
        );

        when(followRepository.findByUserId(
                eq(USER_ID), any(Pageable.class)
        )).thenReturn(emptyPage);

        ResponseDTO<List<FollowCompanyResponse>> response =
                followCompanyService.getMyFollowCompanies(0, 0);

        assertThat(response).isNotNull();
        assertThat(response.getResult()).isEmpty();

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);

        verify(followRepository).findByUserId(
                eq(USER_ID), captor.capture()
        );

        Pageable actual = captor.getValue();

        assertThat(actual.getPageNumber()).isZero();
        assertThat(actual.getPageSize()).isEqualTo(10);
        assertThat(actual.getSort().getOrderFor("followedAt"))
                .isNotNull();
        assertThat(actual.getSort().getOrderFor("followedAt").getDirection())
                .isEqualTo(Sort.Direction.DESC);
    }

    @Test
    void getMyFollowCompanies_shouldReturnEmptyList_whenNoFollowExists() {
        when(followRepository.findByUserId(
                eq(USER_ID), any(Pageable.class)
        )).thenReturn(new PageImpl<>(List.of()));

        ResponseDTO<List<FollowCompanyResponse>> response =
                followCompanyService.getMyFollowCompanies(1, 10);

        assertThat(response).isNotNull();
        assertThat(response.getResult()).isEmpty();
    }

    // ==================== enableNotification ====================

    @Test
    void enableNotification_shouldEnable_whenCurrentlyDisabled() {
        FollowCompany follow = new FollowCompany();
        follow.setId(FOLLOW_ID);
        follow.setNotificationEnabled(false);

        when(followRepository.findByIdAndUserId(
                FOLLOW_ID, USER_ID
        )).thenReturn(Optional.of(follow));

        followCompanyService.enableNotification(FOLLOW_ID);

        assertThat(follow.isNotificationEnabled()).isTrue();

        verify(followRepository).save(follow);
    }

    @Test
    void enableNotification_shouldThrow_whenAlreadyEnabled() {
        FollowCompany follow = new FollowCompany();
        follow.setNotificationEnabled(true);

        when(followRepository.findByIdAndUserId(
                FOLLOW_ID, USER_ID
        )).thenReturn(Optional.of(follow));

        assertThatThrownBy(
                () -> followCompanyService.enableNotification(FOLLOW_ID)
        )
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Notification is already enabled");

        verify(followRepository, never()).save(any());
    }

    @Test
    void enableNotification_shouldThrow_whenFollowNotFound() {
        when(followRepository.findByIdAndUserId(
                FOLLOW_ID, USER_ID
        )).thenReturn(Optional.empty());

        assertThatThrownBy(
                () -> followCompanyService.enableNotification(FOLLOW_ID)
        )
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Follow company not found");

        verify(followRepository, never()).save(any());
    }

    // ==================== disableNotification ====================

    @Test
    void disableNotification_shouldDisable_whenCurrentlyEnabled() {
        FollowCompany follow = new FollowCompany();
        follow.setId(FOLLOW_ID);
        follow.setNotificationEnabled(true);

        when(followRepository.findByIdAndUserId(
                FOLLOW_ID, USER_ID
        )).thenReturn(Optional.of(follow));

        followCompanyService.disableNotification(FOLLOW_ID);

        assertThat(follow.isNotificationEnabled()).isFalse();

        verify(followRepository).save(follow);
    }

    @Test
    void disableNotification_shouldThrow_whenAlreadyDisabled() {
        FollowCompany follow = new FollowCompany();
        follow.setNotificationEnabled(false);

        when(followRepository.findByIdAndUserId(
                FOLLOW_ID, USER_ID
        )).thenReturn(Optional.of(follow));

        assertThatThrownBy(
                () -> followCompanyService.disableNotification(FOLLOW_ID)
        )
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Notification is already disabled");

        verify(followRepository, never()).save(any());
    }

    @Test
    void disableNotification_shouldThrow_whenFollowNotFound() {
        when(followRepository.findByIdAndUserId(
                FOLLOW_ID, USER_ID
        )).thenReturn(Optional.empty());

        assertThatThrownBy(
                () -> followCompanyService.disableNotification(FOLLOW_ID)
        )
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Follow company not found");

        verify(followRepository, never()).save(any());
    }
}
