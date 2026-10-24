package com.webjob.application.service;

import com.webjob.application.component.SecurityUtils;
import com.webjob.application.dto.Request.JobAlertRequest;
import com.webjob.application.dto.Response.JobAlertResponse;
import com.webjob.application.dto.Response.ResponseDTO;
import com.webjob.application.enums.AlertFrequency;
import com.webjob.application.exception.Customs.BadRequestException;
import com.webjob.application.exception.Customs.ResourceNotFoundException;
import com.webjob.application.models.Entity.JobAlert;
import com.webjob.application.models.Entity.JobCategory;
import com.webjob.application.models.Entity.User;
import com.webjob.application.repository.JobAlertRepository;
import com.webjob.application.repository.JobCategoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;
import org.redisson.RedissonMultiLock;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("JobAlertService Unit Tests")
public class JobAlertServiceTest {

    private static final Long USER_ID = 10L;
    private static final Long ALERT_ID = 100L;
    private static final Long CATEGORY_ID = 5L;

    @Mock
    private JobAlertRepository jobAlertRepository;

    @Mock
    private ModelMapper modelMapper;

    @Mock
    private SecurityUtils securityUtils;

    @Mock
    private JobCategoryRepository categoryRepository;

    @Mock
    private RedissonClient redissonClient;

    @Mock
    private RLock userLock;

    @Mock
    private RLock filterLock;


    @InjectMocks
    private JobAlertService jobAlertService;

    private JobAlertRequest request;
    private JobAlert alert;
    private JobCategory category;
    private User user;

    @BeforeEach
    void setUp() throws InterruptedException {
        request = new JobAlertRequest();
        request.setKeyword("Java");
        request.setLocation("Ha Noi");
        request.setSalaryMin(1000.0);
        request.setSalaryMax(3000.0);
        request.setFrequency(AlertFrequency.DAILY);

        alert = new JobAlert();
        alert.setKeyword("Java");
        alert.setLocation("Ha Noi");
        alert.setActive(true);
        alert.setFrequency(AlertFrequency.DAILY);

        category = mock(JobCategory.class);
        lenient().when(category.getId()).thenReturn(CATEGORY_ID);
        lenient().when(category.getName()).thenReturn("IT");

        user = mock(User.class);

        lenient().when(securityUtils.getCurrentUserId()).thenReturn(USER_ID);
        lenient().when(securityUtils.getCurrentUser()).thenReturn(user);


    }

    private void mockAlertFound() {
        when(jobAlertRepository.findByIdAndUserId(ALERT_ID, USER_ID))
                .thenReturn(Optional.of(alert));
    }


    @Test
    void create_shouldSaveAlert_whenRequestIsValid() {
        when(redissonClient.getLock(anyString()))
                .thenReturn(userLock, filterLock);
        when(jobAlertRepository.countByUserId(USER_ID)).thenReturn(0L);
        when(jobAlertRepository.existsByUserIdAndKeywordAndLocationAndJobCategoryIdAndLevelAndWorkMode(
                anyLong(), any(), any(), any(), any(), any()
        )).thenReturn(false);

        when(jobAlertRepository.save(any(JobAlert.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        try (MockedConstruction<RedissonMultiLock> mocked =
                     Mockito.mockConstruction(
                             RedissonMultiLock.class,
                             (mock, context) -> {
                                 when(mock.tryLock(3, 10, TimeUnit.SECONDS))
                                         .thenReturn(true);
                                 when(mock.isHeldByCurrentThread())
                                         .thenReturn(true);
                             })) {

            JobAlertResponse response = jobAlertService.create(request);


            assertThat(response).isNotNull();

            verify(jobAlertRepository).save(argThat(saved ->
                    "Java".equals(saved.getKeyword())
                            && "Ha Noi".equals(saved.getLocation())
                            && saved.getUser() == user
                            && saved.getLastCheckedAt() != null
                            && saved.getNextRunAt() != null
            ));
        }

    }

    @Test
    void create_shouldNormalizeKeywordAndLocation() {
        when(redissonClient.getLock(anyString()))
                .thenReturn(userLock, filterLock);

        request.setKeyword("  Java  ");
        request.setLocation("  Ha Noi  ");

        when(jobAlertRepository.countByUserId(USER_ID)).thenReturn(0L);
        when(jobAlertRepository.existsByUserIdAndKeywordAndLocationAndJobCategoryIdAndLevelAndWorkMode(
                anyLong(), any(), any(), any(), any(), any()
        )).thenReturn(false);
        when(jobAlertRepository.save(any(JobAlert.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        try (MockedConstruction<RedissonMultiLock> mocked =
                     Mockito.mockConstruction(
                             RedissonMultiLock.class,
                             (mock, context) -> {
                                 when(mock.tryLock(3, 10, TimeUnit.SECONDS))
                                         .thenReturn(true);
                                 when(mock.isHeldByCurrentThread())
                                         .thenReturn(true);
                             })) {

            jobAlertService.create(request);

            verify(jobAlertRepository).save(argThat(saved ->
                    "Java".equals(saved.getKeyword())
                            && "Ha Noi".equals(saved.getLocation())
            ));
        }


    }


    @Test
    void create_shouldReject_whenUserHasReachedLimit() {
        when(redissonClient.getLock(anyString()))
                .thenReturn(userLock, filterLock);
        when(jobAlertRepository.countByUserId(USER_ID)).thenReturn(5L);
        try (MockedConstruction<RedissonMultiLock> mocked =
                     Mockito.mockConstruction(
                             RedissonMultiLock.class,
                             (mock, context) -> {
                                 when(mock.tryLock(3, 10, TimeUnit.SECONDS))
                                         .thenReturn(true);
                                 when(mock.isHeldByCurrentThread())
                                         .thenReturn(true);
                             })) {

            assertThatThrownBy(() -> jobAlertService.create(request))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessage("Maximum number of job alerts reached.");


            verify(jobAlertRepository, never()).save(any());

        }

    }

    @Test
    void create_shouldReject_whenSalaryMinIsGreaterThanSalaryMax() {
        when(redissonClient.getLock(anyString()))
                .thenReturn(userLock, filterLock);
        request.setSalaryMin(5000.0);
        request.setSalaryMax(1000.0);

        when(jobAlertRepository.countByUserId(USER_ID)).thenReturn(0L);
        try (MockedConstruction<RedissonMultiLock> mocked =
                     Mockito.mockConstruction(
                             RedissonMultiLock.class,
                             (mock, context) -> {
                                 when(mock.tryLock(3, 10, TimeUnit.SECONDS))
                                         .thenReturn(true);
                                 when(mock.isHeldByCurrentThread())
                                         .thenReturn(true);
                             })) {
            assertThatThrownBy(() -> jobAlertService.create(request))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessage("Salary minimum cannot be greater than salary maximum.");

            verify(jobAlertRepository, never()).save(any());

        }


    }

    @Test
    void create_shouldReject_whenAlertAlreadyExists() {
        when(redissonClient.getLock(anyString()))
                .thenReturn(userLock, filterLock);
        when(jobAlertRepository.countByUserId(USER_ID)).thenReturn(0L);
        when(jobAlertRepository.existsByUserIdAndKeywordAndLocationAndJobCategoryIdAndLevelAndWorkMode(
                anyLong(), any(), any(), any(), any(), any()
        )).thenReturn(true);

        try (MockedConstruction<RedissonMultiLock> mocked =
                     Mockito.mockConstruction(
                             RedissonMultiLock.class,
                             (mock, context) -> {
                                 when(mock.tryLock(3, 10, TimeUnit.SECONDS))
                                         .thenReturn(true);
                                 when(mock.isHeldByCurrentThread())
                                         .thenReturn(true);
                             })) {

            assertThatThrownBy(() -> jobAlertService.create(request))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessage("Job alert already exists.");

            verify(jobAlertRepository, never()).save(any());
        }


    }

    @Test
    void create_shouldReject_whenCategoryDoesNotExist() {
        when(redissonClient.getLock(anyString()))
                .thenReturn(userLock, filterLock);
        request.setJobCategoryId(CATEGORY_ID);

        when(jobAlertRepository.countByUserId(USER_ID)).thenReturn(0L);
        when(categoryRepository.findById(CATEGORY_ID))
                .thenReturn(Optional.empty());

        try (MockedConstruction<RedissonMultiLock> mocked =
                     Mockito.mockConstruction(
                             RedissonMultiLock.class,
                             (mock, context) -> {
                                 when(mock.tryLock(3, 10, TimeUnit.SECONDS))
                                         .thenReturn(true);
                                 when(mock.isHeldByCurrentThread())
                                         .thenReturn(true);
                             })) {

            assertThatThrownBy(() -> jobAlertService.create(request))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("JobCategory not found");

            verify(jobAlertRepository, never()).save(any());

        }

    }


    @Test
    void create_shouldAttachCategory_whenCategoryExists() {
        when(redissonClient.getLock(anyString()))
                .thenReturn(userLock, filterLock);
        request.setJobCategoryId(CATEGORY_ID);

        when(jobAlertRepository.countByUserId(USER_ID)).thenReturn(0L);
        when(categoryRepository.findById(CATEGORY_ID))
                .thenReturn(Optional.of(category));
        when(jobAlertRepository.existsByUserIdAndKeywordAndLocationAndJobCategoryIdAndLevelAndWorkMode(
                anyLong(), any(), any(), any(), any(), any()
        )).thenReturn(false);
        when(jobAlertRepository.save(any(JobAlert.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        try (MockedConstruction<RedissonMultiLock> mocked =
                     Mockito.mockConstruction(
                             RedissonMultiLock.class,
                             (mock, context) -> {
                                 when(mock.tryLock(3, 10, TimeUnit.SECONDS))
                                         .thenReturn(true);
                                 when(mock.isHeldByCurrentThread())
                                         .thenReturn(true);
                             })) {
            JobAlertResponse response = jobAlertService.create(request);

            verify(jobAlertRepository).save(argThat(saved ->
                    saved.getJobCategory() == category
            ));

            assertThat(response.getJobCategoryId()).isEqualTo(CATEGORY_ID);
            assertThat(response.getJobCategoryName()).isEqualTo("IT");
        }
    }


    @Test
    void create_shouldNormalizeBlankValuesToNull() {
        when(redissonClient.getLock(anyString()))
                .thenReturn(userLock, filterLock);
        request.setKeyword("   ");
        request.setLocation("   ");

        when(jobAlertRepository.countByUserId(USER_ID)).thenReturn(0L);
        when(jobAlertRepository.existsByUserIdAndKeywordAndLocationAndJobCategoryIdAndLevelAndWorkMode(
                anyLong(), any(), any(), any(), any(), any()
        )).thenReturn(false);
        when(jobAlertRepository.save(any(JobAlert.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        try (MockedConstruction<RedissonMultiLock> mocked =
                     Mockito.mockConstruction(
                             RedissonMultiLock.class,
                             (mock, context) -> {
                                 when(mock.tryLock(3, 10, TimeUnit.SECONDS))
                                         .thenReturn(true);
                                 when(mock.isHeldByCurrentThread())
                                         .thenReturn(true);
                             })) {
            jobAlertService.create(request);

            verify(jobAlertRepository).save(argThat(saved ->
                    saved.getKeyword() == null && saved.getLocation() == null
            ));

        }

    }

    @Test
    void update_shouldUpdateAlert_whenRequestIsValid() {
        when(redissonClient.getLock(anyString()))
                .thenReturn(userLock);
        mockAlertFound();

        when(jobAlertRepository.save(any(JobAlert.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        try (MockedConstruction<RedissonMultiLock> mocked =
                     Mockito.mockConstruction(
                             RedissonMultiLock.class,
                             (mock, context) -> {
                                 when(mock.tryLock(3, 10, TimeUnit.SECONDS))
                                         .thenReturn(true);
                                 when(mock.isHeldByCurrentThread())
                                         .thenReturn(true);
                             })) {
            JobAlertResponse response = jobAlertService.update(ALERT_ID, request);

            assertThat(response).isNotNull();

            verify(jobAlertRepository).save(argThat(saved ->
                    "Java".equals(saved.getKeyword())
                            && "Ha Noi".equals(saved.getLocation())
                            && saved.getLastCheckedAt() != null
            ));

        }


    }

    @Test
    void update_shouldThrow_whenAlertDoesNotBelongToUser() {
        when(redissonClient.getLock(anyString()))
                .thenReturn(userLock);
        when(jobAlertRepository.findByIdAndUserId(ALERT_ID, USER_ID))
                .thenReturn(Optional.empty());
        try (MockedConstruction<RedissonMultiLock> mocked =
                     Mockito.mockConstruction(
                             RedissonMultiLock.class,
                             (mock, context) -> {
                                 when(mock.tryLock(3, 10, TimeUnit.SECONDS))
                                         .thenReturn(true);
                                 when(mock.isHeldByCurrentThread())
                                         .thenReturn(true);
                             })) {
            assertThatThrownBy(() -> jobAlertService.update(ALERT_ID, request))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Job Alert Not found");

            verify(jobAlertRepository, never()).save(any());

        }


    }

    @Test
    void update_shouldReject_whenSalaryIsInvalid() {
        when(redissonClient.getLock(anyString()))
                .thenReturn(userLock);
        mockAlertFound();
        request.setSalaryMin(5000.0);
        request.setSalaryMax(1000.0);

        try (MockedConstruction<RedissonMultiLock> mocked =
                     Mockito.mockConstruction(
                             RedissonMultiLock.class,
                             (mock, context) -> {
                                 when(mock.tryLock(3, 10, TimeUnit.SECONDS))
                                         .thenReturn(true);
                                 when(mock.isHeldByCurrentThread())
                                         .thenReturn(true);
                             })) {
            assertThatThrownBy(() -> jobAlertService.update(ALERT_ID, request))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessage("Salary minimum cannot be greater than salary maximum.");

            verify(jobAlertRepository, never()).save(any());
        }


    }

    @Test
    void update_shouldThrow_whenCategoryDoesNotExist() {
        when(redissonClient.getLock(anyString()))
                .thenReturn(userLock);
        mockAlertFound();
        request.setJobCategoryId(CATEGORY_ID);

        when(categoryRepository.findById(CATEGORY_ID))
                .thenReturn(Optional.empty());
        try (MockedConstruction<RedissonMultiLock> mocked =
                     Mockito.mockConstruction(
                             RedissonMultiLock.class,
                             (mock, context) -> {
                                 when(mock.tryLock(3, 10, TimeUnit.SECONDS))
                                         .thenReturn(true);
                                 when(mock.isHeldByCurrentThread())
                                         .thenReturn(true);
                             })) {
            assertThatThrownBy(() -> jobAlertService.update(ALERT_ID, request))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("JobCategory not found");

            verify(jobAlertRepository, never()).save(any());

        }


    }

    @Test
    void update_shouldChangeFrequencyAndRecalculateNextRunAt() {
        when(redissonClient.getLock(anyString()))
                .thenReturn(userLock);
        mockAlertFound();

        alert.setFrequency(AlertFrequency.WEEKLY);
        request.setFrequency(AlertFrequency.DAILY);

        when(jobAlertRepository.save(any(JobAlert.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Instant before = Instant.now();
        try (MockedConstruction<RedissonMultiLock> mocked =
                     Mockito.mockConstruction(
                             RedissonMultiLock.class,
                             (mock, context) -> {
                                 when(mock.tryLock(3, 10, TimeUnit.SECONDS))
                                         .thenReturn(true);
                                 when(mock.isHeldByCurrentThread())
                                         .thenReturn(true);
                             })) {
            jobAlertService.update(ALERT_ID, request);

            Instant after = Instant.now();

            assertThat(alert.getFrequency()).isEqualTo(AlertFrequency.DAILY);
            assertThat(alert.getNextRunAt())
                    .isBetween(
                            before.plus(1, ChronoUnit.DAYS),
                            after.plus(1, ChronoUnit.DAYS)
                    );
        }
    }

    @Test
    void update_shouldKeepFrequency_whenRequestFrequencyIsNull() {
        when(redissonClient.getLock(anyString()))
                .thenReturn(userLock);
        mockAlertFound();

        alert.setFrequency(AlertFrequency.WEEKLY);
        Instant oldNextRunAt = Instant.parse("2026-11-01T00:00:00Z");
        alert.setNextRunAt(oldNextRunAt);

        request.setFrequency(null);

        when(jobAlertRepository.save(any(JobAlert.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        try (MockedConstruction<RedissonMultiLock> mocked =
                     Mockito.mockConstruction(
                             RedissonMultiLock.class,
                             (mock, context) -> {
                                 when(mock.tryLock(3, 10, TimeUnit.SECONDS))
                                         .thenReturn(true);
                                 when(mock.isHeldByCurrentThread())
                                         .thenReturn(true);
                             })) {
            jobAlertService.update(ALERT_ID, request);

            assertThat(alert.getFrequency()).isEqualTo(AlertFrequency.WEEKLY);
            assertThat(alert.getNextRunAt()).isEqualTo(oldNextRunAt);

        }


    }

    @Test
    void update_shouldAttachCategory_whenCategoryIdIsProvided() {
        when(redissonClient.getLock(anyString()))
                .thenReturn(userLock);
        mockAlertFound();

        request.setJobCategoryId(CATEGORY_ID);

        when(categoryRepository.findById(CATEGORY_ID))
                .thenReturn(Optional.of(category));
        when(jobAlertRepository.save(any(JobAlert.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        try (MockedConstruction<RedissonMultiLock> mocked =
                     Mockito.mockConstruction(
                             RedissonMultiLock.class,
                             (mock, context) -> {
                                 when(mock.tryLock(3, 10, TimeUnit.SECONDS))
                                         .thenReturn(true);
                                 when(mock.isHeldByCurrentThread())
                                         .thenReturn(true);
                             })) {
            JobAlertResponse response = jobAlertService.update(ALERT_ID, request);

            assertThat(alert.getJobCategory()).isSameAs(category);
            assertThat(response.getJobCategoryId()).isEqualTo(CATEGORY_ID);
            assertThat(response.getJobCategoryName()).isEqualTo("IT");

        }

    }

    @Test
    void enable_shouldActivateAlert_whenCurrentlyDisabled() {
        mockAlertFound();
        alert.setActive(false);

        jobAlertService.enable(ALERT_ID);

        assertThat(alert.getActive()).isTrue();
        verify(jobAlertRepository).save(alert);
    }

    @Test
    void enable_shouldReject_whenAlreadyEnabled() {
        mockAlertFound();
        alert.setActive(true);

        assertThatThrownBy(() -> jobAlertService.enable(ALERT_ID))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Job Alert is True");

        verify(jobAlertRepository, never()).save(any());
    }

    @Test
    void enable_shouldThrow_whenAlertDoesNotExist() {
        when(jobAlertRepository.findByIdAndUserId(ALERT_ID, USER_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> jobAlertService.enable(ALERT_ID))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Job Alert is already enabled");
    }

    @Test
    void disable_shouldDeactivateAlert_whenCurrentlyEnabled() {
        mockAlertFound();
        alert.setActive(true);

        jobAlertService.disable(ALERT_ID);

        assertThat(alert.getActive()).isFalse();
        verify(jobAlertRepository).save(alert);
    }

    @Test
    void disable_shouldReject_whenAlreadyDisabled() {
        mockAlertFound();
        alert.setActive(false);

        assertThatThrownBy(() -> jobAlertService.disable(ALERT_ID))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Job Alert is already disabled");

        verify(jobAlertRepository, never()).save(any());
    }

    @Test
    void disable_shouldThrow_whenAlertDoesNotExist() {
        when(jobAlertRepository.findByIdAndUserId(ALERT_ID, USER_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> jobAlertService.disable(ALERT_ID))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Job Alert Not found");
    }

    @Test
    void getById_shouldReturnAlert_whenFound() {
        mockAlertFound();


        alert.setJobCategory(category);

        JobAlertResponse response = jobAlertService.getById(ALERT_ID);

        assertThat(response).isNotNull();
        assertThat(response.getJobCategoryId()).isEqualTo(CATEGORY_ID);
        assertThat(response.getJobCategoryName()).isEqualTo("IT");

        verify(jobAlertRepository).findByIdAndUserId(ALERT_ID, USER_ID);
    }

    @Test
    void getById_shouldReturnAlertWithoutCategory() {
        mockAlertFound();
        alert.setJobCategory(null);

        JobAlertResponse response = jobAlertService.getById(ALERT_ID);

        assertThat(response).isNotNull();
        verify(jobAlertRepository).findByIdAndUserId(ALERT_ID, USER_ID);
    }

    @Test
    void getById_shouldThrow_whenAlertDoesNotExist() {
        when(jobAlertRepository.findByIdAndUserId(ALERT_ID, USER_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> jobAlertService.getById(ALERT_ID))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Job Alert Not found");
    }

    @Test
    void delete_shouldDeleteAlert_whenFound() {
        mockAlertFound();

        jobAlertService.delete(ALERT_ID);

        verify(jobAlertRepository).delete(alert);
    }

    @Test
    void delete_shouldThrow_whenAlertDoesNotExist() {
        when(jobAlertRepository.findByIdAndUserId(ALERT_ID, USER_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> jobAlertService.delete(ALERT_ID))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Job Alert Not found");

        verify(jobAlertRepository, never()).delete(any(JobAlert.class));
    }

    @Test
    void getMyAlerts_shouldReturnPagedAlerts() {
        JobAlert secondAlert = new JobAlert();

        Page<JobAlert> page = new PageImpl<>(
                List.of(alert, secondAlert),
                org.springframework.data.domain.PageRequest.of(0, 10),
                2
        );

        when(jobAlertRepository.findByUserId(eq(USER_ID), any(Pageable.class)))
                .thenReturn(page);

        ResponseDTO<List<JobAlertResponse>> response =
                jobAlertService.getMyAlerts(1, 10);

        assertThat(response).isNotNull();
        assertThat(response.getResult()).hasSize(2);
        assertThat(response.getMeta()).isNotNull();

        verify(jobAlertRepository).findByUserId(
                eq(USER_ID),
                argThat(pageable ->
                        pageable.getPageNumber() == 0
                                && pageable.getPageSize() == 10
                                && pageable.getSort().isSorted()
                )
        );
    }

    @Test
    void getMyAlerts_shouldClampPageAndSize() {
        when(jobAlertRepository.findByUserId(eq(USER_ID), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        jobAlertService.getMyAlerts(0, 100);

        verify(jobAlertRepository).findByUserId(
                eq(USER_ID),
                argThat(pageable ->
                        pageable.getPageNumber() == 0
                                && pageable.getPageSize() == 50
                )
        );
    }

    @Test
    void getMyAlerts_shouldClampSizeToMinimumOne() {
        when(jobAlertRepository.findByUserId(eq(USER_ID), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        jobAlertService.getMyAlerts(-1, 0);

        verify(jobAlertRepository).findByUserId(
                eq(USER_ID),
                argThat(pageable ->
                        pageable.getPageNumber() == 0
                                && pageable.getPageSize() == 1
                )
        );
    }


    @Test
    void calculateCreateNextRunAt_shouldReturnNow_whenFrequencyIsNull() {
        Instant before = Instant.now();

        Instant result = jobAlertService.calculateCreateNextRunAt(null);

        Instant after = Instant.now();

        assertThat(result).isBetween(before, after);
    }

    @Test
    void calculateCreateNextRunAt_shouldReturnOneDayLater() {
        Instant before = Instant.now();

        Instant result =
                jobAlertService.calculateCreateNextRunAt(AlertFrequency.DAILY);

        Instant after = Instant.now();

        assertThat(result).isBetween(
                before.plus(1, ChronoUnit.DAYS),
                after.plus(1, ChronoUnit.DAYS)
        );
    }

    @Test
    void calculateCreateNextRunAt_shouldReturnSevenDaysLater() {
        Instant before = Instant.now();

        Instant result =
                jobAlertService.calculateCreateNextRunAt(AlertFrequency.WEEKLY);

        Instant after = Instant.now();

        assertThat(result).isBetween(
                before.plus(7, ChronoUnit.DAYS),
                after.plus(7, ChronoUnit.DAYS)
        );
    }

    @Test
    void create_shouldReject_whenLockCannotBeAcquired() throws InterruptedException {

        try (MockedConstruction<RedissonMultiLock> mocked =
                     Mockito.mockConstruction(
                             RedissonMultiLock.class,
                             (mock, context) -> {
                                 when(mock.tryLock(3, 10, TimeUnit.SECONDS))
                                         .thenReturn(false);

                             })) {
            assertThatThrownBy(() -> jobAlertService.create(request))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessage("System is busy. Please retry.");

            verify(jobAlertRepository, never()).save(any());

        }


    }

    @Test
    void update_shouldReject_whenLockCannotBeAcquired() throws InterruptedException {

        try (MockedConstruction<RedissonMultiLock> mocked =
                     Mockito.mockConstruction(
                             RedissonMultiLock.class,
                             (mock, context) -> {
                                 when(mock.tryLock(3, 10, TimeUnit.SECONDS))
                                         .thenReturn(false);

                             })) {
            assertThatThrownBy(() -> jobAlertService.update(ALERT_ID, request))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessage("System is busy. Please retry.");

            verify(jobAlertRepository, never()).save(any());

        }


    }
}
