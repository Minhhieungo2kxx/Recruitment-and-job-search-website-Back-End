package com.webjob.application.service.application;

import com.webjob.application.component.SecurityUtils;
import com.webjob.application.dto.Request.ApplyRequest;
import com.webjob.application.exception.Customs.AlreadyAppliedException;
import com.webjob.application.exception.Customs.AppException;
import com.webjob.application.exception.Customs.JobExpiredException;
import com.webjob.application.exception.Customs.ResourceLockedException;
import com.webjob.application.mapper.ApplicationMapper;
import com.webjob.application.models.Entity.Application;
import com.webjob.application.models.Entity.Job;
import com.webjob.application.models.Entity.User;
import com.webjob.application.models.Entity.UserResume;
import com.webjob.application.repository.ApplicationRepository;
import com.webjob.application.repository.JobRepository;
import com.webjob.application.repository.TemporaryUploadRepository;
import com.webjob.application.repository.UserResumeRepository;
import com.webjob.application.service.ApplicationService;
import com.webjob.application.service.JobService;
import com.webjob.application.service.OutBox.OutboxService;
import com.webjob.application.service.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.BeforeEach;
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

import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.lenient;
@ExtendWith(MockitoExtension.class)
@DisplayName("ApplicationServiceApply Unit Tests")
//ứng tuyển trùng, công việc hết hạn, lock Redis, chọn CV và phát sự kiện.
public class ApplicationServiceApplyTest {
    @Mock private JobService jobService;
    @Mock private UserService userService;
    @Mock private TemporaryUploadRepository temporaryUploadRepository;
    @Mock private ApplicationEventPublisher eventPublisher;
    @Mock private RedissonClient redissonClient;
    @Mock private ApplicationRepository applicationRepository;
    @Mock private JobRepository jobRepository;
    @Mock private UserResumeRepository userResumeRepository;
    @Mock private ApplicationMapper applicationMapper;
    @Mock private SecurityUtils securityUtils;
    @Mock private OutboxService outboxService;
    @Mock private RLock lock;

    @InjectMocks
    private ApplicationService applicationService;

    private User user;
    private User hr;
    private Job job;
    private UserResume resume;
    private ApplyRequest request;

    @BeforeEach
    void setUp() {
        user = mock(User.class);
        hr = mock(User.class);
        job = mock(Job.class);
        resume = mock(UserResume.class);
        request = mock(ApplyRequest.class);

        lenient().when(user.getId()).thenReturn(1L);
        lenient().when(user.getEmail()).thenReturn("candidate@example.com");
        lenient().when(hr.getId()).thenReturn(2L);

        lenient().when(job.getId()).thenReturn(10L);
        lenient().when(job.getEndDate()).thenReturn(
                Instant.now().plusSeconds(3600)
        );

        lenient().when(request.getJobId()).thenReturn(10L);
        lenient().when(request.getResumeId()).thenReturn(100L);

        lenient().when(securityUtils.getCurrentUser()).thenReturn(user);
        lenient().when(jobService.getById(10L)).thenReturn(job);
        lenient().when(userService.getbyHR(any())).thenReturn(hr);

        lenient().when(redissonClient.getLock("lock:apply:1:10"))
                .thenReturn(lock);
        lenient().when(lock.isHeldByCurrentThread())
                .thenReturn(true);
    }

    private void acquireLock() throws InterruptedException {
        when(lock.tryLock(5, 30, TimeUnit.SECONDS))
                .thenReturn(true);
    }
    @Test
    void apply_shouldThrow_whenLockCannotBeAcquired()
            throws InterruptedException {

        when(lock.tryLock(5, 30, TimeUnit.SECONDS))
                .thenReturn(false);

        assertThrows(
                ResourceLockedException.class,
                () -> applicationService.apply(request)
        );

        verify(applicationRepository, never())
                .save(any(Application.class));
        verify(lock, never()).unlock();
    }
    @Test
    void apply_shouldThrow_whenUserAlreadyApplied()
            throws InterruptedException {

        acquireLock();

        when(applicationRepository
                .existsByUserIdAndJobId(1L, 10L))
                .thenReturn(true);

        assertThrows(
                AlreadyAppliedException.class,
                () -> applicationService.apply(request)
        );

        verify(applicationRepository, never())
                .save(any(Application.class));
        verify(lock).unlock();
    }


    @Test
    void apply_shouldTranslateDuplicateDatabaseError()
            throws InterruptedException {

        acquireLock();

        when(applicationRepository
                .existsByUserIdAndJobId(1L, 10L))
                .thenReturn(false);

        when(userResumeRepository
                .findByIdAndUserId(100L, 1L))
                .thenReturn(Optional.of(resume));

        when(applicationRepository.save(any(Application.class)))
                .thenThrow(new DataIntegrityViolationException(
                        "Unique constraint violation"
                ));

        assertThrows(
                AlreadyAppliedException.class,
                () -> applicationService.apply(request)
        );

        verify(lock).unlock();
    }

    @Test
    void apply_shouldRestoreInterruptFlag_whenLockIsInterrupted()
            throws InterruptedException {

        when(lock.tryLock(5, 30, TimeUnit.SECONDS))
                .thenThrow(new InterruptedException());

        try {
            assertThrows(
                    AppException.class,
                    () -> applicationService.apply(request)
            );

            assertTrue(Thread.currentThread().isInterrupted());
        } finally {
            // Tránh để interrupt flag ảnh hưởng test tiếp theo.
            Thread.interrupted();
        }

        verify(applicationRepository, never())
                .save(any(Application.class));
        verify(lock, never()).unlock();
    }
    @Test
    void apply_shouldNotUnlock_whenLockIsNotOwnedByCurrentThread()
            throws InterruptedException {

        acquireLock();

        when(job.getEndDate()).thenReturn(
                Instant.now().minusSeconds(60)
        );
        when(lock.isHeldByCurrentThread()).thenReturn(false);

        assertThrows(
                JobExpiredException.class,
                () -> applicationService.apply(request)
        );

        verify(lock, never()).unlock();
    }


    @Test
    void apply_shouldThrow_whenSavedResumeDoesNotBelongToUser()
            throws InterruptedException {

        acquireLock();

        when(applicationRepository
                .existsByUserIdAndJobId(1L, 10L))
                .thenReturn(false);

        when(userResumeRepository
                .findByIdAndUserId(100L, 1L))
                .thenReturn(Optional.empty());

        assertThrows(
                AppException.class,
                () -> applicationService.apply(request)
        );

        verify(applicationRepository, never())
                .save(any(Application.class));
        verify(lock).unlock();
    }

    @Test
    void apply_shouldNotSaveApplication_whenResumeCannotBeResolved()
            throws InterruptedException {

        acquireLock();

        when(applicationRepository
                .existsByUserIdAndJobId(1L, 10L))
                .thenReturn(false);

        when(userResumeRepository
                .findByIdAndUserId(100L, 1L))
                .thenReturn(Optional.empty());

        assertThrows(
                AppException.class,
                () -> applicationService.apply(request)
        );

        verify(jobRepository, never())
                .increaseAppliedCount(anyLong());
        verify(eventPublisher, never())
                .publishEvent(any());
    }
    @Test
    void apply_shouldRejectWhenApplicationAlreadyExists_beforeResolvingResume()
            throws InterruptedException {

        acquireLock();

        when(applicationRepository
                .existsByUserIdAndJobId(1L, 10L))
                .thenReturn(true);

        assertThrows(
                AlreadyAppliedException.class,
                () -> applicationService.apply(request)
        );

        verify(userResumeRepository, never())
                .findByIdAndUserId(anyLong(), anyLong());
    }


}
