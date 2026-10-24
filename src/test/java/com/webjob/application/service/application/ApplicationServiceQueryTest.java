package com.webjob.application.service.application;

import com.webjob.application.component.SecurityUtils;
import com.webjob.application.dto.Response.ResponseDTO;
import com.webjob.application.dto.Response.ResumeHistoryResponse;
import com.webjob.application.enums.ResumeStatus;
import com.webjob.application.mapper.ApplicationMapper;
import com.webjob.application.models.Entity.Application;
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

// Import các class trong dự án theo package thực tế
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.redisson.api.RedissonClient;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
@ExtendWith(MockitoExtension.class)
@DisplayName("ApplicationServiceQueryTest Unit Tests")
public class ApplicationServiceQueryTest {
    @Mock
    private ApplicationRepository applicationRepository;

    @Mock
    private SecurityUtils securityUtils;

    @Mock
    private ApplicationMapper applicationMapper;

    @Mock
    private JobService jobService;

    @Mock
    private UserService userService;

    @Mock
    private TemporaryUploadRepository temporaryUploadRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private RedissonClient redissonClient;

    @Mock
    private JobRepository jobRepository;

    @Mock
    private UserResumeRepository userResumeRepository;

    @Mock
    private OutboxService outboxService;

    @InjectMocks
    private ApplicationService applicationService;

    @Test
    void getHistoryAppliedClient_shouldNormalizePageAndSize() {
        when(securityUtils.getCurrentUserId()).thenReturn(1L);

        Page<Application> emptyPage = new PageImpl<>(
                List.of(),
                PageRequest.of(0, 50),
                0
        );

        when(applicationRepository.findAll(
                any(Specification.class),
                any(Pageable.class)
        )).thenReturn(emptyPage);

        ResponseDTO<List<ResumeHistoryResponse>> result =
                applicationService.getHistoryAppliedClient(
                        -5, 100, ResumeStatus.PENDING
                );

        ArgumentCaptor<Pageable> captor =
                ArgumentCaptor.forClass(Pageable.class);

        verify(applicationRepository).findAll(
                any(Specification.class),
                captor.capture()
        );

        assertEquals(0, captor.getValue().getPageNumber());
        assertEquals(50, captor.getValue().getPageSize());
        assertNotNull(result);
    }


}
