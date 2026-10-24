package com.webjob.application.service.application;

import com.webjob.application.component.SecurityUtils;
import com.webjob.application.dto.Request.UpdateApplicationStatusRequest;
import com.webjob.application.dto.Response.ApplicationResponse;
import com.webjob.application.event.dto.ApplicationStatusChangedEvent;
import com.webjob.application.enums.ResumeStatus;
import com.webjob.application.exception.Customs.BadRequestException;
import com.webjob.application.exception.Customs.ForbiddenException;
import com.webjob.application.exception.Customs.ResourceNotFoundException;
import com.webjob.application.mapper.ApplicationMapper;
import com.webjob.application.models.Entity.Application;
import com.webjob.application.models.Entity.Company;
import com.webjob.application.models.Entity.Job;
import com.webjob.application.models.Entity.User;
import com.webjob.application.repository.ApplicationRepository;
import com.webjob.application.service.ApplicationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.context.ApplicationEventPublisher;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ApplicationServiceStatus Unit Tests")
public class ApplicationServiceStatusTest {
    @Mock private ApplicationRepository applicationRepository;
    @Mock private SecurityUtils securityUtils;
    @Mock private ApplicationMapper applicationMapper;
    @Mock private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private ApplicationService applicationService;

    private Application application;
    private User hr;
    private Company company;
    private Job job;
    private User candidate;
    private UpdateApplicationStatusRequest request;
    private ApplicationResponse response;

    @BeforeEach
    void setUp() {
        application = mock(Application.class);
        hr = mock(User.class);
        company = mock(Company.class);
        job = mock(Job.class);
        candidate = mock(User.class);
        request = mock(UpdateApplicationStatusRequest.class);
        response = mock(ApplicationResponse.class);

        lenient().when(securityUtils.getCurrentUser()).thenReturn(hr);
        lenient().when(hr.getCompany()).thenReturn(company);
        lenient().when(company.getId()).thenReturn(20L);

        lenient().when(applicationRepository.findById(100L))
                .thenReturn(Optional.of(application));

        lenient().when(application.getJob()).thenReturn(job);
        lenient().when(job.getCompany()).thenReturn(company);
        lenient().when(application.getStatus()).thenReturn(ResumeStatus.PENDING);
        lenient().when(application.getUser()).thenReturn(candidate);
        lenient().when(candidate.getId()).thenReturn(1L);
        lenient().when(job.getName()).thenReturn("Java Developer");
        lenient().when(company.getName()).thenReturn("Example Company");

        lenient().when(request.getStatus()).thenReturn(ResumeStatus.REVIEWING);
        lenient().when(applicationRepository.save(application))
                .thenReturn(application);
        lenient().when(applicationMapper.toResponseApplication(application))
                .thenReturn(response);
    }
    @Test
    void updateStatusByHr_shouldUpdateAndPublishEvent_whenTransitionIsValid() {
        Application result = application;

        assertDoesNotThrow(() -> {
            ApplicationResponse actual = applicationService.updateStatusByHr(100L, request);

            assertSame(response, actual);
        });

        verify(application).setStatus(ResumeStatus.REVIEWING);
        verify(application).setReviewedBy(hr);
        verify(application).setReviewedAt(any());
        verify(applicationRepository).save(application);
        verify(eventPublisher).publishEvent(any(ApplicationStatusChangedEvent.class));
    }

    @Test
    void updateStatusByHr_shouldReject_whenApplicationDoesNotExist() {
        when(applicationRepository.findById(999L))
                .thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> applicationService.updateStatusByHr(
                        999L, request
                )
        );

        verify(applicationRepository, never()).save(any(Application.class));
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void updateStatusByHr_shouldReject_whenHrHasNoCompany() {
        when(hr.getCompany()).thenReturn(null);

        assertThrows(
                BadRequestException.class,
                () -> applicationService.updateStatusByHr(
                        100L, request
                )
        );

        verify(applicationRepository, never())
                .save(any(Application.class));
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void updateStatusByHr_shouldReject_whenApplicationBelongsToAnotherCompany() {
        Company anotherCompany = mock(Company.class);
        when(anotherCompany.getId()).thenReturn(99L);
        when(job.getCompany()).thenReturn(anotherCompany);

        assertThrows(
                ForbiddenException.class,
                () -> applicationService.updateStatusByHr(
                        100L, request
                )
        );

        verify(applicationRepository, never())
                .save(any(Application.class));
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void updateStatusByHr_shouldRejectInvalidTransition() {
        when(application.getStatus()).thenReturn(ResumeStatus.HIRED);
        when(request.getStatus()).thenReturn(ResumeStatus.PENDING);

        assertThrows(
                BadRequestException.class,
                () -> applicationService.updateStatusByHr(
                        100L, request
                )
        );

        verify(applicationRepository, never())
                .save(any(Application.class));
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void updateStatusByHr_shouldNotPublishEvent_whenStatusIsUnchanged() {
        when(request.getStatus()).thenReturn(ResumeStatus.PENDING);

        applicationService.updateStatusByHr(100L, request);

        verify(applicationRepository).save(application);
        verify(eventPublisher, never()).publishEvent(any());
    }


}
