package com.webjob.application.service.application;

import com.webjob.application.component.SecurityUtils;
import com.webjob.application.dto.Request.OutboxDTO;
import com.webjob.application.dto.record.RecommendationCacheInvalidateEvent;
import com.webjob.application.enums.ResumeStatus;
import com.webjob.application.exception.Customs.BadRequestException;
import com.webjob.application.exception.Customs.ForbiddenException;
import com.webjob.application.exception.Customs.ResourceNotFoundException;
import com.webjob.application.models.Entity.Application;
import com.webjob.application.models.Entity.Job;
import com.webjob.application.models.Entity.User;
import com.webjob.application.repository.ApplicationRepository;
import com.webjob.application.repository.JobRepository;
import com.webjob.application.service.ApplicationService;
import com.webjob.application.service.OutBox.OutboxService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ApplicationServiceDelete Unit Tests")
public class ApplicationServiceDeleteTest {
    @Mock private ApplicationRepository applicationRepository;
    @Mock private JobRepository jobRepository;
    @Mock private SecurityUtils securityUtils;
    @Mock private ApplicationEventPublisher eventPublisher;
    @Mock private OutboxService outboxService;

    @InjectMocks
    private ApplicationService applicationService;

    private Application application;
    private User candidate;
    private Job job;


    @BeforeEach
    void setUp() {
        application = mock(Application.class);
        candidate = mock(User.class);
        job = mock(Job.class);

        lenient().when(securityUtils.getCurrentUserId()).thenReturn(1L);
        lenient().when(applicationRepository.findById(100L))
                .thenReturn(Optional.of(application));

        lenient().when(application.getUser()).thenReturn(candidate);
        lenient().when(candidate.getId()).thenReturn(1L);
        lenient().when(application.getJob()).thenReturn(job);
        lenient().when(job.getId()).thenReturn(10L);
        lenient().when(job.getAppliedCount()).thenReturn(1);
        lenient().when(application.getStatus()).thenReturn(ResumeStatus.PENDING);
    }

    @Test
    void deleteByClient_shouldDeleteAndDecreaseCount_whenApplicationIsPending() {
        applicationService.deleteByClient(100L);

        verify(jobRepository).decreaseAppliedCount(10L);
        verify(applicationRepository).delete(application);
        verify(eventPublisher).publishEvent(
                any(RecommendationCacheInvalidateEvent.class)
        );
        verify(outboxService).save(any(OutboxDTO.class));
    }

    @Test
    void deleteByClient_shouldReject_whenApplicationBelongsToAnotherUser() {
        when(candidate.getId()).thenReturn(2L);

        assertThrows(
                ForbiddenException.class,
                () -> applicationService.deleteByClient(100L)
        );

        verify(applicationRepository, never())
                .delete(any(Application.class));
        verify(jobRepository, never())
                .decreaseAppliedCount(anyLong());
    }
    @Test
    void deleteByClient_shouldReject_whenApplicationIsAlreadyProcessed() {
        when(application.getStatus()).thenReturn(ResumeStatus.REVIEWING);

        assertThrows(
                BadRequestException.class,
                () -> applicationService.deleteByClient(100L)
        );

        verify(applicationRepository, never())
                .delete(any(Application.class));
        verify(jobRepository, never())
                .decreaseAppliedCount(anyLong());
    }
    @Test
    void deleteByClient_shouldReject_whenApplicationDoesNotExist() {
        when(applicationRepository.findById(999L))
                .thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> applicationService.deleteByClient(999L)
        );

        verify(applicationRepository, never())
                .delete(any(Application.class));
    }
    @Test
    void deleteByClient_shouldNotDecreaseCount_whenAppliedCountIsZero() {
        when(job.getAppliedCount()).thenReturn(0);

        applicationService.deleteByClient(100L);

        verify(jobRepository, never())
                .decreaseAppliedCount(anyLong());
        verify(outboxService, never()).save(any(OutboxDTO.class));
        verify(applicationRepository).delete(application);
    }

    @Test
    void deleteByAdmin_shouldDeleteApplicationAndInvalidateCache() {
        applicationService.deleteByAdmin(100L);

        verify(applicationRepository).delete(application);
        verify(jobRepository).decreaseAppliedCount(10L);
        verify(eventPublisher).publishEvent(
                any(RecommendationCacheInvalidateEvent.class)
        );
    }
}
