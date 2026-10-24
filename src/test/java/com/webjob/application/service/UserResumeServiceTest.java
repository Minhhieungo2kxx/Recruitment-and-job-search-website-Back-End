package com.webjob.application.service;

import com.webjob.application.component.SecurityUtils;
import com.webjob.application.dto.Request.UpdateResumeNameRequest;
import com.webjob.application.dto.Request.UploadResumeRequest;
import com.webjob.application.dto.Response.AdminResumeResponse;
import com.webjob.application.dto.Response.ResponseDTO;
import com.webjob.application.dto.Response.UserResumeResponse;
import com.webjob.application.event.ResumeFileDeletedEvent;
import com.webjob.application.dto.record.RecommendationCacheInvalidateEvent;
import com.webjob.application.exception.Customs.AppException;
import com.webjob.application.exception.Customs.BadRequestException;
import com.webjob.application.exception.Customs.ForbiddenException;
import com.webjob.application.exception.Customs.ResourceNotFoundException;
import com.webjob.application.mapper.ApplicationMapper;
import com.webjob.application.mapper.UserResumeMapper;
import com.webjob.application.models.Entity.JobRecommendation;
import com.webjob.application.models.Entity.TemporaryUpload;
import com.webjob.application.models.Entity.User;
import com.webjob.application.models.Entity.UserResume;
import com.webjob.application.repository.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import org.modelmapper.ModelMapper;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.*;
@ExtendWith(MockitoExtension.class)
@DisplayName("UserResumeService Unit Tests")
public class UserResumeServiceTest {
    @Mock
    private UserResumeRepository userResumeRepository;

    @Mock
    private UserRepository userRepository;
    @Mock
    private ApplicationMapper applicationMapper;
    @Mock
    private SecurityUtils securityUtils;
    @Mock
    private UserResumeMapper userResumeMapper;
    @Mock
    private ModelMapper modelMapper;
    @Mock
    private TemporaryUploadRepository temporaryUploadRepository;
    @Mock
    private ApplicationEventPublisher eventPublisher;
    @Mock
    private ApplicationRepository applicationRepository;
    @Mock
    private JobRecommendationRepository jobRecommendationRepository;
    @InjectMocks
    private UserResumeService userResumeService;
    private User currentUser;
    private UserResume resume;
    private UserResume secondResume;
    private TemporaryUpload temporaryUpload;

    private static final Long USER_ID = 1L;
    private static final Long RESUME_ID = 10L;
    private static final Long SECOND_RESUME_ID = 20L;

    @BeforeEach
    void setUp() {
        currentUser = User.builder()
                .id(USER_ID)
                .build();

        resume = UserResume.builder()
                .id(RESUME_ID)
                .name("CV 2026")
                .url("https://example.com/cv.pdf")
                .publicId("cv-public-id")
                .resourceType("raw")
                .isDefault(true)
                .createdAt(Instant.now())
                .user(currentUser)
                .build();

        secondResume = UserResume.builder()
                .id(SECOND_RESUME_ID)
                .name("CV 2026 - 2")
                .url("https://example.com/cv2.pdf")
                .publicId("cv-public-id-2")
                .resourceType("raw")
                .isDefault(false)
                .createdAt(Instant.now())
                .user(currentUser)
                .build();

        temporaryUpload = TemporaryUpload.builder()
                .publicId("cv-public-id")
                .url("https://example.com/cv.pdf")
                .resourceType("raw")
                .used(false)
                .user(currentUser)
                .build();
    }
    // ============================================================
    // getMyResumes()
    // ============================================================

    @Nested
    @DisplayName("getMyResumes()")
    class GetMyResumesTests {
        @Test
        @DisplayName("Should return all resumes of current user")
        void shouldReturnMyResumes() {

            UserResume resume2 = UserResume.builder()
                    .id(20L)
                    .name("CV 2")
                    .user(currentUser)
                    .isDefault(false)
                    .build();

            UserResumeResponse response1 = new UserResumeResponse();
            UserResumeResponse response2 = new UserResumeResponse();

            when(securityUtils.getCurrentUserId())
                    .thenReturn(USER_ID);

            when(userResumeRepository
                    .findAllByUserIdOrderByIsDefaultDescCreatedAtDesc(USER_ID))
                    .thenReturn(List.of(resume, resume2));

            when(applicationMapper.toUserResumeResponse(resume))
                    .thenReturn(response1);

            when(applicationMapper.toUserResumeResponse(resume2))
                    .thenReturn(response2);

            List<UserResumeResponse> result =
                    userResumeService.getMyResumes();

            assertThat(result).containsExactly(response1, response2);

            verify(userResumeRepository)
                    .findAllByUserIdOrderByIsDefaultDescCreatedAtDesc(USER_ID);

            verify(applicationMapper)
                    .toUserResumeResponse(resume);

            verify(applicationMapper)
                    .toUserResumeResponse(resume2);
        }


        @Test
        @DisplayName("Should return empty list when user has no resumes")
        void shouldReturnEmptyListWhenNoResume() {

            when(securityUtils.getCurrentUserId())
                    .thenReturn(USER_ID);

            when(userResumeRepository
                    .findAllByUserIdOrderByIsDefaultDescCreatedAtDesc(USER_ID))
                    .thenReturn(Collections.emptyList());

            List<UserResumeResponse> result =
                    userResumeService.getMyResumes();

            assertThat(result).isEmpty();

            verify(applicationMapper, never())
                    .toUserResumeResponse(any());
        }
    }

    // ============================================================
    // getMyResumesPageList()
    // ============================================================

    @Nested
    @DisplayName("getMyResumesPageList()")
    class GetMyResumesPageListTests {

        @Test
        @DisplayName("Should return paginated resumes")
        void shouldReturnPaginatedResumes() {

            UserResumeResponse response = new UserResumeResponse();

            Page<UserResume> page = new PageImpl<>(
                    List.of(resume),
                    PageRequest.of(
                            1,
                            10,
                            Sort.by(Sort.Direction.DESC, "createdAt")
                    ),
                    21
            );

            when(securityUtils.getCurrentUserId())
                    .thenReturn(USER_ID);

            when(userResumeRepository.findByUserId(
                    eq(USER_ID),
                    any(Pageable.class)
            )).thenReturn(page);

            when(applicationMapper.toUserResumeResponse(resume))
                    .thenReturn(response);

            ResponseDTO<List<UserResumeResponse>> result =
                    userResumeService.getMyResumesPageList(2, 10);

            assertThat(result).isNotNull();
            assertThat(result.getResult())
                    .containsExactly(response);

            ArgumentCaptor<Pageable> pageableCaptor =
                    ArgumentCaptor.forClass(Pageable.class);

            verify(userResumeRepository)
                    .findByUserId(eq(USER_ID), pageableCaptor.capture());

            Pageable pageable = pageableCaptor.getValue();

            assertThat(pageable.getPageNumber())
                    .isEqualTo(1);

            assertThat(pageable.getPageSize())
                    .isEqualTo(10);

            assertThat(pageable.getSort())
                    .isEqualTo(
                            Sort.by(
                                    Sort.Direction.DESC,
                                    "createdAt"
                            )
                    );
        }


        @Test
        @DisplayName("Should normalize page <= 0 to page 1")
        void shouldNormalizeInvalidPage() {

            Page<UserResume> page = new PageImpl<>(List.of(resume));

            when(securityUtils.getCurrentUserId())
                    .thenReturn(USER_ID);

            when(userResumeRepository.findByUserId(
                    eq(USER_ID),
                    any(Pageable.class)
            )).thenReturn(page);

            userResumeService.getMyResumesPageList(0, 10);

            ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);

            verify(userResumeRepository).findByUserId(eq(USER_ID), captor.capture());

            assertThat(captor.getValue().getPageNumber()).isEqualTo(0);
        }


        @Test
        @DisplayName("Should normalize negative page to page 1")
        void shouldNormalizeNegativePage() {

            Page<UserResume> page =
                    new PageImpl<>(List.of(resume));

            when(securityUtils.getCurrentUserId())
                    .thenReturn(USER_ID);

            when(userResumeRepository.findByUserId(
                    eq(USER_ID),
                    any(Pageable.class)
            )).thenReturn(page);

            userResumeService.getMyResumesPageList(-5, 10);

            ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);

            verify(userResumeRepository)
                    .findByUserId(eq(USER_ID), captor.capture());

            assertThat(captor.getValue().getPageNumber()).isEqualTo(0);
        }


        @Test
        @DisplayName("Should normalize size <= 0 to size 10")
        void shouldNormalizeInvalidSize() {

            Page<UserResume> page =
                    new PageImpl<>(List.of(resume));

            when(securityUtils.getCurrentUserId())
                    .thenReturn(USER_ID);

            when(userResumeRepository.findByUserId(
                    eq(USER_ID),
                    any(Pageable.class)
            )).thenReturn(page);

            userResumeService.getMyResumesPageList(1, 0);

            ArgumentCaptor<Pageable> captor =
                    ArgumentCaptor.forClass(Pageable.class);

            verify(userResumeRepository)
                    .findByUserId(eq(USER_ID), captor.capture());

            assertThat(captor.getValue().getPageSize())
                    .isEqualTo(10);
        }


        @Test
        @DisplayName("Should return empty result when user has no resumes")
        void shouldReturnEmptyResult() {

            Page<UserResume> emptyPage =
                    new PageImpl<>(
                            Collections.emptyList(),
                            PageRequest.of(0, 10),
                            0
                    );

            when(securityUtils.getCurrentUserId())
                    .thenReturn(USER_ID);

            when(userResumeRepository.findByUserId(
                    eq(USER_ID),
                    any(Pageable.class)
            )).thenReturn(emptyPage);

            ResponseDTO<List<UserResumeResponse>> result =
                    userResumeService.getMyResumesPageList(1, 10);

            assertThat(result).isNotNull();
            assertThat(result.getResult()).isEmpty();
        }
    }

    // ============================================================
    // getAllCvAdmin()
    // ============================================================

    @Nested
    @DisplayName("getAllCvAdmin()")
    class GetAllCvAdminTests {

        @Test
        @DisplayName("Should return paginated admin resumes")
        void shouldReturnAdminResumes() {

            AdminResumeResponse response = new AdminResumeResponse();

            Page<AdminResumeResponse> page =
                    new PageImpl<>(
                            List.of(response),
                            PageRequest.of(0, 10),
                            1
                    );

            when(userResumeRepository.getAllResumeForAdmin(
                    any(Pageable.class)
            )).thenReturn(page);

            when(userResumeMapper.toAdminResumeResponse(
                    response
            )).thenReturn(response);

            ResponseDTO<List<AdminResumeResponse>> result =
                    userResumeService.getAllCvAdmin(1, 10);

            assertThat(result).isNotNull();
            assertThat(result.getResult())
                    .containsExactly(response);

            verify(userResumeRepository)
                    .getAllResumeForAdmin(any(Pageable.class));
        }


        @Test
        @DisplayName("Should normalize invalid page and size")
        void shouldNormalizeInvalidPagination() {

            Page<AdminResumeResponse> page =
                    new PageImpl<>(Collections.emptyList());

            when(userResumeRepository.getAllResumeForAdmin(
                    any(Pageable.class)
            )).thenReturn(page);

            userResumeService.getAllCvAdmin(0, 0);

            ArgumentCaptor<Pageable> captor =
                    ArgumentCaptor.forClass(Pageable.class);

            verify(userResumeRepository)
                    .getAllResumeForAdmin(captor.capture());

            Pageable pageable = captor.getValue();

            assertThat(pageable.getPageNumber())
                    .isEqualTo(0);

            assertThat(pageable.getPageSize())
                    .isEqualTo(10);
        }
    }

    // ============================================================
    // getById()
    // ============================================================

    @Nested
    @DisplayName("getById()")
    class GetByIdTests {

        @Test
        @DisplayName("Should return resume by id")
        void shouldReturnResumeById() {

            UserResumeResponse response = new UserResumeResponse();

            when(userResumeRepository.findById(RESUME_ID))
                    .thenReturn(Optional.of(resume));

            when(modelMapper.map(
                    resume,
                    UserResumeResponse.class
            )).thenReturn(response);

            UserResumeResponse result = userResumeService.getById(RESUME_ID);

            assertThat(result).isSameAs(response);

            verify(userResumeRepository).findById(RESUME_ID);

            verify(modelMapper).map(resume, UserResumeResponse.class);
        }


        @Test
        @DisplayName("Should throw ResourceNotFoundException when resume does not exist")
        void shouldThrowWhenResumeNotFound() {

            when(userResumeRepository.findById(RESUME_ID))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() ->
                    userResumeService.getById(RESUME_ID)
            )
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("UserResume not found");

            verify(modelMapper, never())
                    .map(any(), eq(UserResumeResponse.class));
        }
    }

    // ============================================================
    // createResume()
    // ============================================================

    @Nested
    @DisplayName("createResume()")
    class CreateResumeTests {

        @Test
        @DisplayName("Should create first resume as default")
        void shouldCreateFirstResumeAsDefault() {

            UploadResumeRequest request = mock(UploadResumeRequest.class);

            when(request.getPublicId()).thenReturn("cv-public-id");

//            when(request.getIsDefault()).thenReturn(false);

            when(securityUtils.getCurrentUser()).thenReturn(currentUser);

            // Giả lập trả về một Optional chứa user thành công
            when(userRepository.lockUser(currentUser.getId())).thenReturn(Optional.of(currentUser));

            when(temporaryUploadRepository
                    .findByPublicIdAndUsedFalse("cv-public-id"))
                    .thenReturn(Optional.of(temporaryUpload));

            when(userResumeRepository.countByUserId(USER_ID)).thenReturn(0L);

            when(userResumeRepository.save(any(UserResume.class)))
                    .thenAnswer(invocation ->
                            invocation.getArgument(0));

            UserResumeResponse response = new UserResumeResponse();

            when(modelMapper.map(
                    any(UserResume.class),
                    eq(UserResumeResponse.class)
            )).thenReturn(response);

            UserResumeResponse result = userResumeService.createResume(request);

            assertThat(result).isSameAs(response);

            ArgumentCaptor<UserResume> captor = ArgumentCaptor.forClass(UserResume.class);

            verify(userResumeRepository).save(captor.capture());

            UserResume savedResume = captor.getValue();

            assertThat(savedResume.getUser()).isEqualTo(currentUser);

            assertThat(savedResume.getPublicId())
                    .isEqualTo("cv-public-id");

            assertThat(savedResume.getUrl())
                    .isEqualTo(temporaryUpload.getUrl());

            assertThat(savedResume.getResourceType())
                    .isEqualTo(temporaryUpload.getResourceType());

            assertThat(savedResume.getIsDefault()).isTrue();

            verify(userResumeRepository).clearDefaultResume(USER_ID);

            verify(temporaryUploadRepository).save(temporaryUpload);

            assertThat(temporaryUpload.isUsed()).isTrue();

            verify(eventPublisher)
                    .publishEvent(any(RecommendationCacheInvalidateEvent.class));
        }


        @Test
        @DisplayName("Should create non-default resume when user already has resumes")
        void shouldCreateNonDefaultResume() {

            UploadResumeRequest request =
                    mock(UploadResumeRequest.class);

            when(request.getPublicId())
                    .thenReturn("cv-public-id");

            when(request.getIsDefault())
                    .thenReturn(false);

            when(securityUtils.getCurrentUser())
                    .thenReturn(currentUser);

            // Giả lập trả về một Optional chứa user thành công
            when(userRepository.lockUser(currentUser.getId())).thenReturn(Optional.of(currentUser));

            when(temporaryUploadRepository
                    .findByPublicIdAndUsedFalse("cv-public-id"))
                    .thenReturn(Optional.of(temporaryUpload));

            when(userResumeRepository.countByUserId(USER_ID))
                    .thenReturn(1L);

            when(userResumeRepository.save(any(UserResume.class)))
                    .thenAnswer(invocation ->
                            invocation.getArgument(0));

            when(modelMapper.map(
                    any(UserResume.class),
                    eq(UserResumeResponse.class)
            )).thenReturn(new UserResumeResponse());

            userResumeService.createResume(request);

            ArgumentCaptor<UserResume> captor =
                    ArgumentCaptor.forClass(UserResume.class);

            verify(userResumeRepository)
                    .save(captor.capture());

            assertThat(captor.getValue().getIsDefault())
                    .isFalse();

            verify(userResumeRepository, never())
                    .clearDefaultResume(USER_ID);

            verify(eventPublisher, never())
                    .publishEvent(any(RecommendationCacheInvalidateEvent.class));
        }


        @Test
        @DisplayName("Should make new resume default when request isDefault = true")
        void shouldCreateDefaultResumeWhenRequested() {

            UploadResumeRequest request = mock(UploadResumeRequest.class);

            when(request.getPublicId()).thenReturn("cv-public-id");

            when(request.getIsDefault()).thenReturn(true);

            when(securityUtils.getCurrentUser()).thenReturn(currentUser);

            when(temporaryUploadRepository
                    .findByPublicIdAndUsedFalse("cv-public-id"))
                    .thenReturn(Optional.of(temporaryUpload));

            when(userResumeRepository.countByUserId(USER_ID))
                    .thenReturn(2L);

            when(userResumeRepository.save(any(UserResume.class)))
                    .thenAnswer(invocation ->
                            invocation.getArgument(0));

            when(modelMapper.map(
                    any(UserResume.class),
                    eq(UserResumeResponse.class)
            )).thenReturn(new UserResumeResponse());

            userResumeService.createResume(request);

            ArgumentCaptor<UserResume> captor =
                    ArgumentCaptor.forClass(UserResume.class);

            verify(userResumeRepository)
                    .save(captor.capture());

            assertThat(captor.getValue().getIsDefault())
                    .isTrue();

            verify(userResumeRepository)
                    .clearDefaultResume(USER_ID);

            verify(eventPublisher)
                    .publishEvent(any(RecommendationCacheInvalidateEvent.class));
        }


        @Test
        @DisplayName("Should throw when temporary upload does not exist")
        void shouldThrowWhenUploadNotFound() {

            UploadResumeRequest request =
                    mock(UploadResumeRequest.class);

            when(request.getPublicId())
                    .thenReturn("not-exist");

            when(securityUtils.getCurrentUser())
                    .thenReturn(currentUser);

            when(temporaryUploadRepository
                    .findByPublicIdAndUsedFalse("not-exist"))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() ->
                    userResumeService.createResume(request)
            )
                    .isInstanceOf(AppException.class)
                    .hasMessage("File không tồn tại.");

            verify(userResumeRepository, never())
                    .save(any());

            verify(temporaryUploadRepository, never())
                    .save(any());
        }


        @Test
        @DisplayName("Should throw when upload belongs to another user")
        void shouldThrowWhenUploadBelongsToAnotherUser() {

            User anotherUser = User.builder()
                    .id(999L)
                    .build();

            temporaryUpload.setUser(anotherUser);

            UploadResumeRequest request =
                    mock(UploadResumeRequest.class);

            when(request.getPublicId())
                    .thenReturn("cv-public-id");

            when(securityUtils.getCurrentUser())
                    .thenReturn(currentUser);

            when(temporaryUploadRepository
                    .findByPublicIdAndUsedFalse("cv-public-id"))
                    .thenReturn(Optional.of(temporaryUpload));

            assertThatThrownBy(() ->
                    userResumeService.createResume(request)
            )
                    .isInstanceOf(AppException.class)
                    .hasMessage("CV không thuộc người dùng.");

            verify(userResumeRepository, never())
                    .countByUserId(anyLong());

            verify(userResumeRepository, never())
                    .save(any());
        }


        @Test
        @DisplayName("Should reject sixth resume")
        void shouldRejectWhenResumeLimitReached() {

            UploadResumeRequest request =
                    mock(UploadResumeRequest.class);

            when(request.getPublicId())
                    .thenReturn("cv-public-id");

            when(securityUtils.getCurrentUser())
                    .thenReturn(currentUser);

            when(temporaryUploadRepository
                    .findByPublicIdAndUsedFalse("cv-public-id"))
                    .thenReturn(Optional.of(temporaryUpload));

            when(userResumeRepository.countByUserId(USER_ID))
                    .thenReturn(5L);

            assertThatThrownBy(() ->
                    userResumeService.createResume(request)
            )
                    .isInstanceOf(BadRequestException.class)
                    .hasMessage("Bạn chỉ được lưu tối đa 5 CV.");

            verify(userResumeRepository, never())
                    .save(any());

            verify(temporaryUploadRepository, never())
                    .save(any());
        }
    }

    // ============================================================
    // updateResumeName()
    // ============================================================

    @Nested
    @DisplayName("updateResumeName()")
    class UpdateResumeNameTests {

        @Test
        @DisplayName("Should update resume name")
        void shouldUpdateResumeName() {

            UpdateResumeNameRequest request = mock(UpdateResumeNameRequest.class);

            when(request.getName()).thenReturn("CV Backend Developer");

            when(securityUtils.getCurrentUser())
                    .thenReturn(currentUser);

            when(userResumeRepository
                    .findByIdAndUserId(
                            RESUME_ID,
                            USER_ID
                    ))
                    .thenReturn(Optional.of(resume));

            when(userResumeRepository.save(resume))
                    .thenReturn(resume);

            UserResumeResponse response = new UserResumeResponse();

            when(modelMapper.map(
                    resume,
                    UserResumeResponse.class
            )).thenReturn(response);

            UserResumeResponse result =
                    userResumeService.updateResumeName(
                            RESUME_ID,
                            request
                    );

            assertThat(result)
                    .isSameAs(response);

            assertThat(resume.getName())
                    .isEqualTo("CV Backend Developer");

            verify(userResumeRepository)
                    .save(resume);
        }


        @Test
        @DisplayName("Should throw when resume does not belong to current user")
        void shouldThrowWhenResumeNotOwnedByUser() {

            UpdateResumeNameRequest request =
                    mock(UpdateResumeNameRequest.class);

            when(securityUtils.getCurrentUser())
                    .thenReturn(currentUser);

            when(userResumeRepository
                    .findByIdAndUserId(
                            RESUME_ID,
                            USER_ID
                    ))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() ->
                    userResumeService.updateResumeName(
                            RESUME_ID,
                            request
                    )
            )
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Không tìm thấy CV.");

            verify(userResumeRepository, never())
                    .save(any());
        }
    }
    // ============================================================
    // setDefaultResume()
    // ============================================================

    @Nested
    @DisplayName("setDefaultResume()")
    class SetDefaultResumeTests {
        @Test
        @DisplayName("Should return immediately when resume is already default")
        void shouldReturnImmediatelyWhenAlreadyDefault() {

            resume.setIsDefault(true);

            when(securityUtils.getCurrentUser()).thenReturn(currentUser);

            when(userResumeRepository
                    .findByIdAndUserId(
                            RESUME_ID,
                            USER_ID
                    ))
                    .thenReturn(Optional.of(resume));

            UserResumeResponse response =
                    new UserResumeResponse();

            when(modelMapper.map(
                    resume,
                    UserResumeResponse.class
            )).thenReturn(response);

            UserResumeResponse result =
                    userResumeService.setDefaultResume(RESUME_ID);

            assertThat(result)
                    .isSameAs(response);

            verify(userResumeRepository, never())
                    .clearDefaultResume(USER_ID);

            verify(userResumeRepository, never())
                    .save(any());

            verify(eventPublisher, never())
                    .publishEvent(any(RecommendationCacheInvalidateEvent.class));
        }


        @Test
        @DisplayName("Should change default resume")
        void shouldChangeDefaultResume() {

            resume.setIsDefault(false);

            when(securityUtils.getCurrentUser())
                    .thenReturn(currentUser);

            when(userResumeRepository
                    .findByIdAndUserId(
                            RESUME_ID,
                            USER_ID
                    ))
                    .thenReturn(Optional.of(resume));

            when(userResumeRepository.save(resume))
                    .thenReturn(resume);

            UserResumeResponse response =
                    new UserResumeResponse();

            when(modelMapper.map(
                    resume,
                    UserResumeResponse.class
            )).thenReturn(response);

            UserResumeResponse result =
                    userResumeService.setDefaultResume(RESUME_ID);

            assertThat(result)
                    .isSameAs(response);

            assertThat(resume.getIsDefault())
                    .isTrue();

            verify(userResumeRepository)
                    .clearDefaultResume(USER_ID);

            verify(userResumeRepository)
                    .save(resume);

            verify(eventPublisher)
                    .publishEvent(any(RecommendationCacheInvalidateEvent.class));
        }


        @Test
        @DisplayName("Should throw when resume is not found for current user")
        void shouldThrowWhenResumeNotFound() {

            when(securityUtils.getCurrentUser())
                    .thenReturn(currentUser);

            when(userResumeRepository
                    .findByIdAndUserId(
                            RESUME_ID,
                            USER_ID
                    ))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() ->
                    userResumeService.setDefaultResume(RESUME_ID)
            )
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Không tìm thấy CV.");

            verify(userResumeRepository, never())
                    .clearDefaultResume(anyLong());

            verify(eventPublisher, never())
                    .publishEvent(any());
        }
    }

    // ============================================================
    // deleteMyResume()
    // ============================================================

    @Nested
    @DisplayName("deleteMyResume()")
    class DeleteMyResumeTests {

        @Test
        @DisplayName("Should delete user's own resume")
        void shouldDeleteOwnResume() {

            resume.setIsDefault(false);

            when(userResumeRepository.findById(RESUME_ID))
                    .thenReturn(Optional.of(resume));

            when(securityUtils.getCurrentUserId())
                    .thenReturn(USER_ID);

            when(applicationRepository.existsByResumeId(RESUME_ID))
                    .thenReturn(false);

            when(temporaryUploadRepository
                    .findByPublicId("cv-public-id"))
                    .thenReturn(Optional.of(temporaryUpload));

            when(jobRecommendationRepository
                    .findByResumeId(RESUME_ID))
                    .thenReturn(Collections.emptyList());

            userResumeService.deleteMyResume(RESUME_ID);

            verify(userResumeRepository)
                    .delete(resume);

            verify(temporaryUploadRepository)
                    .delete(temporaryUpload);

            verify(eventPublisher)
                    .publishEvent(any(ResumeFileDeletedEvent.class));
        }


        @Test
        @DisplayName("Should throw when resume does not exist")
        void shouldThrowWhenResumeDoesNotExist() {

            when(userResumeRepository.findById(RESUME_ID))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() ->
                    userResumeService.deleteMyResume(RESUME_ID)
            )
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Resume không tồn tại");

            verify(applicationRepository, never())
                    .existsByResumeId(anyLong());

            verify(userResumeRepository, never())
                    .delete(any());
        }


        @Test
        @DisplayName("Should throw ForbiddenException when resume belongs to another user")
        void shouldThrowWhenUserDoesNotOwnResume() {

            User anotherUser = User.builder()
                    .id(999L)
                    .build();

            resume.setUser(anotherUser);

            when(userResumeRepository.findById(RESUME_ID))
                    .thenReturn(Optional.of(resume));

            when(securityUtils.getCurrentUserId())
                    .thenReturn(USER_ID);

            assertThatThrownBy(() ->
                    userResumeService.deleteMyResume(RESUME_ID)
            )
                    .isInstanceOf(ForbiddenException.class)
                    .hasMessage("Bạn không có quyền xóa CV này.");

            verify(applicationRepository, never())
                    .existsByResumeId(anyLong());

            verify(userResumeRepository, never())
                    .delete(any());
        }


        @Test
        @DisplayName("Should reject deletion when resume is used by application")
        void shouldRejectWhenResumeIsUsedForApplication() {

            when(userResumeRepository.findById(RESUME_ID))
                    .thenReturn(Optional.of(resume));

            when(securityUtils.getCurrentUserId())
                    .thenReturn(USER_ID);

            when(applicationRepository.existsByResumeId(RESUME_ID))
                    .thenReturn(true);

            assertThatThrownBy(() ->
                    userResumeService.deleteMyResume(RESUME_ID)
            )
                    .isInstanceOf(BadRequestException.class)
                    .hasMessage("CV đang được sử dụng để ứng tuyển.");

            verify(userResumeRepository, never())
                    .delete(any());

            verify(temporaryUploadRepository, never())
                    .delete(any());
        }
    }


    // ============================================================
    // deleteResumeByAdmin()
    // ============================================================

    @Nested
    @DisplayName("deleteResumeByAdmin()")
    class DeleteResumeByAdminTests {

        @Test
        @DisplayName("Should allow admin to delete resume")
        void shouldDeleteResumeByAdmin() {

            resume.setIsDefault(false);

            when(userResumeRepository.findById(RESUME_ID))
                    .thenReturn(Optional.of(resume));

            when(applicationRepository.existsByResumeId(RESUME_ID))
                    .thenReturn(false);

            when(temporaryUploadRepository
                    .findByPublicId("cv-public-id"))
                    .thenReturn(Optional.of(temporaryUpload));

            when(jobRecommendationRepository
                    .findByResumeId(RESUME_ID))
                    .thenReturn(Collections.emptyList());

            userResumeService.deleteResumeByAdmin(RESUME_ID);

            verify(userResumeRepository)
                    .delete(resume);

            verify(temporaryUploadRepository)
                    .delete(temporaryUpload);

            verify(eventPublisher)
                    .publishEvent(any(ResumeFileDeletedEvent.class));
        }


        @Test
        @DisplayName("Should throw when admin tries to delete non-existing resume")
        void shouldThrowWhenAdminResumeNotFound() {

            when(userResumeRepository.findById(RESUME_ID))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() ->
                    userResumeService.deleteResumeByAdmin(RESUME_ID)
            )
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Resume không tồn tại");

            verify(userResumeRepository, never())
                    .delete(any());
        }
    }


    // ============================================================
    // deleteResumeInternal() behavior through public methods
    // ============================================================

    @Nested
    @DisplayName("Delete internal business rules")
    class DeleteInternalTests {

        @Test
        @DisplayName("Should delete related job recommendations before deleting resume")
        void shouldDeleteRelatedRecommendations() {

            resume.setIsDefault(false);

            JobRecommendation recommendation1 = mock(JobRecommendation.class);

            JobRecommendation recommendation2 = mock(JobRecommendation.class);

            when(userResumeRepository.findById(RESUME_ID))
                    .thenReturn(Optional.of(resume));

            when(applicationRepository.existsByResumeId(RESUME_ID))
                    .thenReturn(false);

            when(jobRecommendationRepository.findByResumeId(RESUME_ID))
                    .thenReturn(List.of(
                            recommendation1,
                            recommendation2
                    ));

            when(temporaryUploadRepository
                    .findByPublicId("cv-public-id"))
                    .thenReturn(Optional.empty());

            userResumeService.deleteResumeByAdmin(RESUME_ID);

            verify(jobRecommendationRepository)
                    .deleteAll(
                            List.of(
                                    recommendation1,
                                    recommendation2
                            )
                    );

            verify(userResumeRepository)
                    .delete(resume);
        }


        @Test
        @DisplayName("Should not delete recommendations when none exist")
        void shouldNotDeleteRecommendationsWhenEmpty() {

            resume.setIsDefault(false);

            when(userResumeRepository.findById(RESUME_ID))
                    .thenReturn(Optional.of(resume));

            when(applicationRepository.existsByResumeId(RESUME_ID))
                    .thenReturn(false);

            when(jobRecommendationRepository.findByResumeId(RESUME_ID))
                    .thenReturn(Collections.emptyList());

            when(temporaryUploadRepository
                    .findByPublicId("cv-public-id"))
                    .thenReturn(Optional.empty());

            userResumeService.deleteResumeByAdmin(RESUME_ID);

            verify(jobRecommendationRepository, never())
                    .deleteAll(anyList());

            verify(userResumeRepository)
                    .delete(resume);
        }


        @Test
        @DisplayName("Should delete resume even when TemporaryUpload no longer exists")
        void shouldDeleteWhenTemporaryUploadNotFound() {

            resume.setIsDefault(false);

            when(userResumeRepository.findById(RESUME_ID))
                    .thenReturn(Optional.of(resume));

            when(applicationRepository.existsByResumeId(RESUME_ID))
                    .thenReturn(false);

            when(jobRecommendationRepository.findByResumeId(RESUME_ID))
                    .thenReturn(Collections.emptyList());

            when(temporaryUploadRepository
                    .findByPublicId("cv-public-id"))
                    .thenReturn(Optional.empty());

            userResumeService.deleteResumeByAdmin(RESUME_ID);

            verify(userResumeRepository)
                    .delete(resume);

            verify(temporaryUploadRepository, never())
                    .delete(any());
        }


        @Test
        @DisplayName("Should promote newest resume when deleting default resume")
        void shouldPromoteNewestResumeWhenDeletingDefault() {

            resume.setIsDefault(true);

            UserResume newestResume =
                    UserResume.builder()
                            .id(SECOND_RESUME_ID)
                            .name("Newest CV")
                            .user(currentUser)
                            .isDefault(false)
                            .build();

            when(userResumeRepository.findById(RESUME_ID))
                    .thenReturn(Optional.of(resume));

            when(applicationRepository.existsByResumeId(RESUME_ID))
                    .thenReturn(false);

            when(jobRecommendationRepository.findByResumeId(RESUME_ID))
                    .thenReturn(Collections.emptyList());

            when(temporaryUploadRepository
                    .findByPublicId("cv-public-id"))
                    .thenReturn(Optional.empty());

            when(userResumeRepository
                    .findFirstByUserIdOrderByCreatedAtDescIdDesc(USER_ID))
                    .thenReturn(Optional.of(newestResume));

            userResumeService.deleteResumeByAdmin(RESUME_ID);

            assertThat(newestResume.getIsDefault())
                    .isTrue();

            verify(userResumeRepository)
                    .save(newestResume);

            verify(eventPublisher)
                    .publishEvent(
                            any(RecommendationCacheInvalidateEvent.class)
                    );

            verify(eventPublisher)
                    .publishEvent(
                            any(ResumeFileDeletedEvent.class)
                    );
        }


        @Test
        @DisplayName("Should invalidate recommendation cache when deleting default resume and no resume remains")
        void shouldInvalidateCacheWhenNoResumeRemains() {

            resume.setIsDefault(true);

            when(userResumeRepository.findById(RESUME_ID))
                    .thenReturn(Optional.of(resume));

            when(applicationRepository.existsByResumeId(RESUME_ID))
                    .thenReturn(false);

            when(jobRecommendationRepository.findByResumeId(RESUME_ID))
                    .thenReturn(Collections.emptyList());

            when(temporaryUploadRepository
                    .findByPublicId("cv-public-id"))
                    .thenReturn(Optional.empty());

            when(userResumeRepository
                    .findFirstByUserIdOrderByCreatedAtDescIdDesc(USER_ID))
                    .thenReturn(Optional.empty());

            userResumeService.deleteResumeByAdmin(RESUME_ID);

            verify(userResumeRepository).delete(resume);

            verify(userResumeRepository, never()).save(any(UserResume.class));

            verify(eventPublisher)
                    .publishEvent(
                            any(RecommendationCacheInvalidateEvent.class)
                    );

            verify(eventPublisher)
                    .publishEvent(
                            any(ResumeFileDeletedEvent.class)
                    );
        }


        @Test
        @DisplayName("Should not invalidate recommendation cache when deleting non-default resume")
        void shouldNotInvalidateCacheWhenDeletingNonDefault() {

            resume.setIsDefault(false);

            when(userResumeRepository.findById(RESUME_ID))
                    .thenReturn(Optional.of(resume));

            when(applicationRepository.existsByResumeId(RESUME_ID))
                    .thenReturn(false);

            when(jobRecommendationRepository.findByResumeId(RESUME_ID))
                    .thenReturn(Collections.emptyList());

            when(temporaryUploadRepository
                    .findByPublicId("cv-public-id"))
                    .thenReturn(Optional.empty());

            userResumeService.deleteResumeByAdmin(RESUME_ID);

            verify(eventPublisher, never())
                    .publishEvent(
                            any(RecommendationCacheInvalidateEvent.class)
                    );

            verify(eventPublisher)
                    .publishEvent(
                            any(ResumeFileDeletedEvent.class)
                    );
        }
    }
    // ============================================================
    // Event tests
    // ============================================================

    @Nested
    @DisplayName("Events")
    class EventTests {

        @Test
        @DisplayName("Should publish recommendation cache invalidation event")
        void shouldPublishRecommendationCacheInvalidateEvent() {

            userResumeService.publishRecommendationCacheInvalidateEvent(
                    USER_ID,
                    "CV mặc định thay đổi"
            );

            ArgumentCaptor<RecommendationCacheInvalidateEvent>
                    captor = ArgumentCaptor.forClass(RecommendationCacheInvalidateEvent.class);

            verify(eventPublisher).publishEvent(captor.capture());

            RecommendationCacheInvalidateEvent event = captor.getValue();

            assertThat(event).isNotNull();
            assertThat(event.userId()).isEqualTo(USER_ID);
            assertThat(event.reason()).isEqualTo("CV mặc định thay đổi");

        }
    }

}
