package com.webjob.application.service;

import com.webjob.application.component.SecurityUtils;
import com.webjob.application.dto.Response.ResponseDTO;
import com.webjob.application.dto.Response.SavedJobResponse;
import com.webjob.application.dto.record.RecommendationCacheInvalidateEvent;
import com.webjob.application.exception.Customs.AppException;
import com.webjob.application.exception.Customs.BadRequestException;
import com.webjob.application.exception.Customs.ResourceLockedException;
import com.webjob.application.exception.Customs.ResourceNotFoundException;
import com.webjob.application.mapper.SavedJobMapper;
import com.webjob.application.models.Entity.Job;
import com.webjob.application.models.Entity.SavedJob;
import com.webjob.application.models.Entity.User;
import com.webjob.application.repository.JobRepository;
import com.webjob.application.repository.SavedJobRepository;
import com.webjob.application.repository.UserRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.result.StatusResultMatchersExtensionsKt.isEqualTo;

@ExtendWith(MockitoExtension.class)
@DisplayName("SavedJobService Unit Tests")
public class SavedJobServiceTest {

    private static final Long USER_ID = 1L;
    private static final Long JOB_ID = 10L;

    @Mock
    private SavedJobRepository savedJobRepository;

    @Mock
    private SecurityUtils utils;

    @Mock
    private UserRepository userRepository;

    @Mock
    private JobRepository jobRepository;

    @Mock
    private SavedJobMapper savedJobMapper;

    @Mock
    private RedissonClient redissonClient;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private RLock lock;

    @InjectMocks
    private SavedJobService savedJobService;

    private User user;
    private Job job;

    @BeforeEach
    void setUp() throws InterruptedException {
        user = new User();
        user.setId(USER_ID);

        job = new Job();
        job.setId(JOB_ID);

        lenient().when(utils.getCurrentUser()).thenReturn(user);
        lenient().when(utils.getCurrentUserId()).thenReturn(USER_ID);
        lenient().when(redissonClient.getLock("lock:saveJob:1:10")).thenReturn(lock);
        lenient().when(lock.tryLock(5, 30, TimeUnit.SECONDS)).thenReturn(true);
        lenient().when(lock.isHeldByCurrentThread()).thenReturn(true);
    }

    // --------------------------------------------------
    // saveJob
    // --------------------------------------------------

    @Test
    void saveJob_shouldSaveJobAndPublishEvent_whenLockAcquired() throws Exception {
        // Arrange
        when(jobRepository.findByIdAndDeletedFalse(JOB_ID))
                .thenReturn(Optional.of(job));

        // Act
        savedJobService.saveJob(JOB_ID);

        // Assert
        ArgumentCaptor<SavedJob> captor = ArgumentCaptor.forClass(SavedJob.class);

        verify(savedJobRepository).save(captor.capture());

        assertThat(captor.getValue().getUser()).isSameAs(user);
        assertThat(captor.getValue().getJob()).isSameAs(job);

        verify(eventPublisher).publishEvent(any(RecommendationCacheInvalidateEvent.class));
        verify(lock).unlock();
    }

    @Test
    void saveJob_shouldThrowResourceLockedException_whenLockNotAcquired()
            throws Exception {
        when(lock.tryLock(5, 30, TimeUnit.SECONDS)).thenReturn(false);

        // Act & Assert
        assertThatThrownBy(() -> savedJobService.saveJob(JOB_ID))
                .isInstanceOf(ResourceLockedException.class)
                .hasMessageContaining("Vui lòng thử lại");

        verifyNoInteractions(jobRepository, savedJobRepository, eventPublisher);
        verify(lock, never()).unlock();
    }

    @Test
    void saveJob_shouldThrowResourceNotFoundException_whenJobDoesNotExist()
            throws Exception {
        // Arrange

        when(jobRepository.findByIdAndDeletedFalse(JOB_ID))
                .thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> savedJobService.saveJob(JOB_ID))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Job not found");

        verify(savedJobRepository, never()).save(any(SavedJob.class));
        verify(eventPublisher, never()).publishEvent(any());
        verify(lock).unlock();
    }

    @Test
    void saveJob_shouldThrowBadRequestException_whenJobAlreadySaved()
            throws Exception {
        // Arrange
        when(jobRepository.findByIdAndDeletedFalse(JOB_ID))
                .thenReturn(Optional.of(job));

        when(savedJobRepository.save(any(SavedJob.class)))
                .thenThrow(new DataIntegrityViolationException("Duplicate"));

        // Act & Assert
        assertThatThrownBy(() -> savedJobService.saveJob(JOB_ID))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Bạn đã lưu công việc này.");

        verify(eventPublisher, never()).publishEvent(any());
        verify(lock).unlock();
    }

    @Test
    void saveJob_shouldRestoreInterruptFlag_whenLockAcquisitionIsInterrupted()
            throws Exception {
        // Arrange
        when(lock.tryLock(5, 30, TimeUnit.SECONDS))
                .thenThrow(new InterruptedException("Interrupted"));

        // Act & Assert
        try {
            assertThatThrownBy(() -> savedJobService.saveJob(JOB_ID))
                    .isInstanceOf(AppException.class)
                    .hasMessage("Không thể lấy lock.");

            assertThat(Thread.currentThread().isInterrupted()).isTrue();

            verifyNoInteractions(jobRepository, savedJobRepository, eventPublisher);
            verify(lock, never()).unlock();
        } finally {
            // Không để interrupt flag ảnh hưởng các test chạy sau.
            Thread.interrupted();
        }
    }


    // --------------------------------------------------
    // unsaveJob
    // --------------------------------------------------

    @Test
    void unsaveJob_shouldDeleteSavedJobAndPublishEvent_whenSavedJobExists() {
        // Arrange
        SavedJob savedJob = new SavedJob();

        when(savedJobRepository.findByUserIdAndJobId(USER_ID, JOB_ID))
                .thenReturn(Optional.of(savedJob));

        // Act
        savedJobService.unsaveJob(JOB_ID);

        // Assert
        verify(savedJobRepository).delete(savedJob);
        verify(eventPublisher).publishEvent(
                any(RecommendationCacheInvalidateEvent.class));
    }

    @Test
    void unsaveJob_shouldThrowResourceNotFoundException_whenSavedJobDoesNotExist() {
        // Arrange

        when(savedJobRepository.findByUserIdAndJobId(USER_ID, JOB_ID))
                .thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> savedJobService.unsaveJob(JOB_ID))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Saved job not found");

        verify(savedJobRepository, never()).delete(any(SavedJob.class));
        verifyNoInteractions(eventPublisher);
    }

    // --------------------------------------------------
    // getSavedJobs
    // --------------------------------------------------

    @Test
    void getSavedJobs_shouldReturnMappedResultsAndPaginationMetadata() {
        // Arrange
        int page = 2;
        int size = 5;

        SavedJob savedJob = new SavedJob();
        SavedJobResponse response = mock(SavedJobResponse.class);

        Page<SavedJob> savedJobPage = new PageImpl<>(
                List.of(savedJob),
                PageRequest.of(1, size, Sort.by("savedAt").descending()),
                6
        );

        when(savedJobRepository.findByUserId(eq(USER_ID), any(Pageable.class)))
                .thenReturn(savedJobPage);
        when(savedJobMapper.fromEntity(savedJob)).thenReturn(response);

        // Act
        ResponseDTO<List<SavedJobResponse>> result = savedJobService.getSavedJobs(page, size);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getResult()).containsExactly(response);

        assertThat(result.getMeta().getCurrent()).isEqualTo(2);
        assertThat(result.getMeta().getPageSize()).isEqualTo(5);
        assertThat(result.getMeta().getTotalPages()).isEqualTo(2);
        assertThat(result.getMeta().getTotalItems()).isEqualTo(6L);

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);

        verify(savedJobRepository).findByUserId(
                eq(USER_ID), pageableCaptor.capture());

        Pageable pageable = pageableCaptor.getValue();

        assertThat(pageable.getPageNumber()).isEqualTo(1);
        assertThat(pageable.getPageSize()).isEqualTo(5);
        assertThat(pageable.getSort().getOrderFor("savedAt"))
                .isNotNull()
                .extracting(Sort.Order::getDirection)
                .isEqualTo(Sort.Direction.DESC);

        verify(savedJobMapper).fromEntity(savedJob);
    }

    @Test
    void getSavedJobs_shouldUseDefaultPageAndSize_whenArgumentsAreInvalid() {
        // Arrange
        when(utils.getCurrentUserId()).thenReturn(USER_ID);
        when(savedJobRepository.findByUserId(eq(USER_ID), any(Pageable.class)))
                .thenReturn(Page.empty(PageRequest.of(0, 10)));

        // Act
        savedJobService.getSavedJobs(0, -1);

        // Assert
        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);

        verify(savedJobRepository).findByUserId(
                eq(USER_ID), pageableCaptor.capture());

        Pageable pageable = pageableCaptor.getValue();

        assertThat(pageable.getPageNumber()).isZero();
        assertThat(pageable.getPageSize()).isEqualTo(1);
        assertThat(pageable.getSort().getOrderFor("savedAt"))
                .isNotNull()
                .extracting(Sort.Order::getDirection)
                .isEqualTo(Sort.Direction.DESC);

        verifyNoInteractions(savedJobMapper);
    }

    @Test
    void getSavedJobs_shouldReturnEmptyList_whenUserHasNoSavedJobs() {
        // Arrange
        when(utils.getCurrentUserId()).thenReturn(USER_ID);
        when(savedJobRepository.findByUserId(eq(USER_ID), any(Pageable.class)))
                .thenReturn(Page.empty(PageRequest.of(0, 10)));

        // Act
        ResponseDTO<List<SavedJobResponse>> result =
                savedJobService.getSavedJobs(1, 10);

        // Assert
        assertThat(result.getResult()).isEmpty();
        verifyNoInteractions(savedJobMapper);
    }
    // --------------------------------------------------
    // publishRecommendationCacheInvalidateEvent
    // --------------------------------------------------

    @Test
    void publishRecommendationCacheInvalidateEvent_shouldPublishEvent() {
        // Arrange
        String message = "saved_job";

        // Act
        savedJobService.publishRecommendationCacheInvalidateEvent(USER_ID, message);

        // Assert
        verify(eventPublisher).publishEvent(
                any(RecommendationCacheInvalidateEvent.class));
    }

    @Test
    void saveJob_shouldNotUnlock_whenCurrentThreadDoesNotHoldLock()
            throws Exception {
        when(utils.getCurrentUser()).thenReturn(user);
        when(redissonClient.getLock("lock:saveJob:1:10")).thenReturn(lock);
        when(lock.tryLock(5, 30, TimeUnit.SECONDS)).thenReturn(true);
        when(lock.isHeldByCurrentThread()).thenReturn(false);
        when(jobRepository.findByIdAndDeletedFalse(JOB_ID))
                .thenReturn(Optional.of(job));

        savedJobService.saveJob(JOB_ID);

        verify(savedJobRepository).save(any(SavedJob.class));
        verify(lock, never()).unlock();
    }

    @Test
    void saveJob_shouldPublishCacheInvalidationEventWithCorrectPayload()
            throws Exception {
        when(jobRepository.findByIdAndDeletedFalse(JOB_ID))
                .thenReturn(Optional.of(job));

        savedJobService.saveJob(JOB_ID);

        ArgumentCaptor<RecommendationCacheInvalidateEvent> captor = ArgumentCaptor.forClass(RecommendationCacheInvalidateEvent.class);

        verify(eventPublisher).publishEvent(captor.capture());

        RecommendationCacheInvalidateEvent event = captor.getValue();

        // Điều chỉnh tên getter theo class event thực tế.
        assertThat(event.userId()).isEqualTo(USER_ID);
        assertThat(event.reason()).isEqualTo("saved_job");
    }

    @Test
    void saveJob_shouldPropagateUnexpectedPersistenceException()
            throws Exception {
        RuntimeException persistenceError = new RuntimeException("Database unavailable");

//
        when(jobRepository.findByIdAndDeletedFalse(JOB_ID))
                .thenReturn(Optional.of(job));
        when(savedJobRepository.save(any(SavedJob.class)))
                .thenThrow(persistenceError);

        assertThatThrownBy(() -> savedJobService.saveJob(JOB_ID))
                .isSameAs(persistenceError);

        verify(eventPublisher, never()).publishEvent(any());
        verify(lock).unlock();
    }

    @Test
    void getSavedJobs_shouldUseDefaultPagination_whenPageIsNegativeAndSizeIsZero() {
        when(utils.getCurrentUserId()).thenReturn(USER_ID);
        when(savedJobRepository.findByUserId(eq(USER_ID), any(Pageable.class)))
                .thenReturn(Page.empty(PageRequest.of(0, 10)));

        savedJobService.getSavedJobs(-1, 0);

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);

        verify(savedJobRepository).findByUserId(eq(USER_ID), captor.capture());

        assertThat(captor.getValue().getPageNumber()).isZero();
        assertThat(captor.getValue().getPageSize()).isEqualTo(1);
    }
    @Test
    void getSavedJobs_shouldMapAllItemsInRepositoryOrder() {
        SavedJob first = new SavedJob();
        SavedJob second = new SavedJob();

        SavedJobResponse firstResponse = mock(SavedJobResponse.class);
        SavedJobResponse secondResponse = mock(SavedJobResponse.class);

        when(savedJobRepository.findByUserId(eq(USER_ID), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(first, second)));

        when(savedJobMapper.fromEntity(first)).thenReturn(firstResponse);
        when(savedJobMapper.fromEntity(second)).thenReturn(secondResponse);

        ResponseDTO<List<SavedJobResponse>> result = savedJobService.getSavedJobs(1, 10);

        assertThat(result.getResult()).containsExactly(firstResponse, secondResponse);

        verify(savedJobMapper).fromEntity(first);
        verify(savedJobMapper).fromEntity(second);
    }

}
