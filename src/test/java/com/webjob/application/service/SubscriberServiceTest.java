package com.webjob.application.service;

import com.webjob.application.component.SecurityUtils;
import com.webjob.application.dto.Request.Search.SubscriberFilterRequest;
import com.webjob.application.dto.Request.SubscriberRequest;
import com.webjob.application.dto.Response.ResponseDTO;
import com.webjob.application.dto.Response.SubscriberListResponse;
import com.webjob.application.dto.Response.SubscriberResponse;
import com.webjob.application.exception.Customs.BadRequestException;
import com.webjob.application.exception.Customs.ForbiddenException;
import com.webjob.application.exception.Customs.ResourceNotFoundException;
import com.webjob.application.mapper.SubscriberMapper;
import com.webjob.application.models.Entity.Skill;
import com.webjob.application.models.Entity.Subscriber;
import com.webjob.application.models.Entity.SubscriberSkill;
import com.webjob.application.models.Entity.User;
import com.webjob.application.repository.SkillRepository;
import com.webjob.application.repository.SubscriberRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import org.modelmapper.ModelMapper;

import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.redisson.RedissonMultiLock;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("SubscriberService Unit Tests")
public class SubscriberServiceTest {

    private static final Long USER_ID = 1L;
    private static final Long OTHER_USER_ID = 2L;
    private static final Long SUBSCRIBER_ID = 10L;
    private static final Long SKILL_ID = 100L;
    private static final String SUBSCRIBER_NAME = "Java Developer";

    @Mock
    private SubscriberRepository subscriberRepository;

    @Mock
    private SkillRepository skillRepository;

    @Mock
    private ModelMapper modelMapper;

    @Mock
    private SubscriberMapper subscriberMapper;

    @Mock
    private SecurityUtils securityUtils;

    @Mock
    private RedissonClient redissonClient;

    @Mock
    private RLock userLock;

    @Mock
    private RLock nameLock;

    @Mock
    private RLock subscriberLock;

    @InjectMocks
    private SubscriberService subscriberService;

    private User currentUser;
    private User otherUser;
    private Skill skill;
    private Subscriber subscriber;
    private SubscriberRequest request;

    @BeforeEach
    void setUp() throws InterruptedException {
        currentUser = new User();
        currentUser.setId(USER_ID);

        otherUser = new User();
        otherUser.setId(OTHER_USER_ID);

        skill = new Skill();
        skill.setId(SKILL_ID);

        request = new SubscriberRequest();
        request.setName(SUBSCRIBER_NAME);
        request.setSkillIds(List.of(SKILL_ID));

        subscriber = new Subscriber();
        subscriber.setId(SUBSCRIBER_ID);
        subscriber.setUser(currentUser);
        subscriber.setSubscriberSkills(new java.util.ArrayList<>());

        lenient().when(securityUtils.getCurrentUserId()).thenReturn(USER_ID);
        lenient().when(securityUtils.getCurrentUser()).thenReturn(currentUser);

        lenient().when(redissonClient.getLock(
                        "subscriber:create:user:" + USER_ID))
                .thenReturn(userLock);

        lenient().when(redissonClient.getLock(
                        "subscriber:name:" + SUBSCRIBER_NAME.toLowerCase()))
                .thenReturn(nameLock);

        lenient().when(redissonClient.getLock(
                        "subscriber:" + SUBSCRIBER_ID))
                .thenReturn(subscriberLock);

        lenient().when(redissonClient.getLock(
                        "subscriber:name:" + SUBSCRIBER_NAME))
                .thenReturn(nameLock);


    }


    // ---------------------------------------------------------
    // createSubscriber
    // ---------------------------------------------------------
    @Test
    void createSubscriber_shouldCreateSubscriberSuccessfully() throws Exception {
        when(subscriberRepository.countByUserId(USER_ID)).thenReturn(2L);
        when(skillRepository.findAllById(List.of(SKILL_ID)))
                .thenReturn(List.of(skill));
        when(subscriberRepository.existsByName(SUBSCRIBER_NAME))
                .thenReturn(false);
        when(subscriberRepository.save(any(Subscriber.class)))
                .thenReturn(subscriber);

        SubscriberResponse expected = mock(SubscriberResponse.class);
        when(subscriberMapper.mapToResponse(subscriber)).thenReturn(expected);

        try (MockedConstruction<RedissonMultiLock> mocked =
                     Mockito.mockConstruction(
                             RedissonMultiLock.class,
                             (mock, context) -> {
                                 when(mock.tryLock(3, 10, TimeUnit.SECONDS))
                                         .thenReturn(true);
                                 when(mock.isHeldByCurrentThread())
                                         .thenReturn(true);
                             })) {
            SubscriberResponse actual = subscriberService.createSubscriber(request);

            assertThat(actual).isSameAs(expected);

            ArgumentCaptor<Subscriber> captor = ArgumentCaptor.forClass(Subscriber.class);

            verify(subscriberRepository).save(captor.capture());

            Subscriber savedArgument = captor.getValue();

            assertThat(savedArgument.getUser()).isSameAs(currentUser);
            assertThat(savedArgument.getSubscriberSkills()).hasSize(1);
            assertThat(savedArgument.getSubscriberSkills().get(0).getSkill())
                    .isSameAs(skill);
            assertThat(savedArgument.getSubscriberSkills().get(0).getSubscriber())
                    .isSameAs(savedArgument);

            // Verify lock behavior
            RedissonMultiLock multiLock = mocked.constructed().get(0);
            verify(multiLock).tryLock(3, 10, TimeUnit.SECONDS);
            verify(multiLock, times(1)).unlock();
        }

    }

    @Test
    void createSubscriber_shouldRejectWhenUserHasFiveSubscribers()
            throws Exception {
        when(subscriberRepository.countByUserId(USER_ID)).thenReturn(5L);
        try (MockedConstruction<RedissonMultiLock> mocked =
                     Mockito.mockConstruction(
                             RedissonMultiLock.class,
                             (mock, context) -> {
                                 when(mock.tryLock(3, 10, TimeUnit.SECONDS))
                                         .thenReturn(true);
                                 when(mock.isHeldByCurrentThread())
                                         .thenReturn(true);
                             })) {
            assertThatThrownBy(
                    () -> subscriberService.createSubscriber(request))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessage("Maximum number of subscriber reached.");
            verify(subscriberRepository, never()).save(any());
            verify(skillRepository, never()).findAllById(any());

            // Verify lock behavior
            RedissonMultiLock multiLock = mocked.constructed().get(0);
            verify(multiLock).tryLock(3, 10, TimeUnit.SECONDS);
            verify(multiLock, times(1)).unlock();

        }


    }

    @Test
    void createSubscriber_shouldRejectWhenAnySkillDoesNotExist()
            throws Exception {
        when(subscriberRepository.countByUserId(USER_ID)).thenReturn(0L);
        when(skillRepository.findAllById(List.of(SKILL_ID)))
                .thenReturn(List.of());

        try (MockedConstruction<RedissonMultiLock> mocked =
                     Mockito.mockConstruction(
                             RedissonMultiLock.class,
                             (mock, context) -> {
                                 when(mock.tryLock(3, 10, TimeUnit.SECONDS))
                                         .thenReturn(true);
                                 when(mock.isHeldByCurrentThread())
                                         .thenReturn(true);
                             })) {

            assertThatThrownBy(
                    () -> subscriberService.createSubscriber(request))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Một hoặc nhiều Skill không tồn tại.");

            verify(subscriberRepository, never()).save(any());
            verify(subscriberRepository, never()).existsByName(anyString());


            // Verify lock behavior
            RedissonMultiLock multiLock = mocked.constructed().get(0);
            verify(multiLock).tryLock(3, 10, TimeUnit.SECONDS);
            verify(multiLock, times(1)).unlock();

        }


    }

    @Test
    void createSubscriber_shouldRejectDuplicateName() throws Exception {
        when(subscriberRepository.countByUserId(USER_ID)).thenReturn(0L);
        when(skillRepository.findAllById(List.of(SKILL_ID)))
                .thenReturn(List.of(skill));
        when(subscriberRepository.existsByName(SUBSCRIBER_NAME))
                .thenReturn(true);

        try (MockedConstruction<RedissonMultiLock> mocked =
                     Mockito.mockConstruction(
                             RedissonMultiLock.class,
                             (mock, context) -> {
                                 when(mock.tryLock(3, 10, TimeUnit.SECONDS))
                                         .thenReturn(true);
                                 when(mock.isHeldByCurrentThread())
                                         .thenReturn(true);
                             })) {


            assertThatThrownBy(
                    () -> subscriberService.createSubscriber(request))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessage("Tên subscriber đã tồn tại.");

            verify(subscriberRepository, never()).save(any());


            // Verify lock behavior
            RedissonMultiLock multiLock = mocked.constructed().get(0);
            verify(multiLock).tryLock(3, 10, TimeUnit.SECONDS);
            verify(multiLock, times(1)).unlock();

        }


    }

    @Test
    void createSubscriber_shouldRejectWhenLockCannotBeAcquired()
            throws Exception {

        try (MockedConstruction<RedissonMultiLock> mocked =
                     Mockito.mockConstruction(
                             RedissonMultiLock.class,
                             (mock, context) -> {
                                 when(mock.tryLock(3, 10, TimeUnit.SECONDS))
                                         .thenReturn(false);
                                 when(mock.isHeldByCurrentThread())
                                         .thenReturn(true);
                             })) {

            assertThatThrownBy(
                    () -> subscriberService.createSubscriber(request))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessage("System is busy. Please retry.");

            verify(subscriberRepository, never()).countByUserId(anyLong());
            verify(subscriberRepository, never()).save(any());

            // Không được unlock nếu không lấy được toàn bộ multi-lock.
            RedissonMultiLock multiLock = mocked.constructed().get(0);
            verify(multiLock).tryLock(3, 10, TimeUnit.SECONDS);
            verify(multiLock, never()).unlock();

        }
    }

    @Test
    void createSubscriber_shouldTranslateDataIntegrityViolation()
            throws Exception {
        when(subscriberRepository.countByUserId(USER_ID)).thenReturn(0L);
        when(skillRepository.findAllById(List.of(SKILL_ID)))
                .thenReturn(List.of(skill));
        when(subscriberRepository.existsByName(SUBSCRIBER_NAME))
                .thenReturn(false);
        when(subscriberRepository.save(any(Subscriber.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate"));
        try (MockedConstruction<RedissonMultiLock> mocked =
                     Mockito.mockConstruction(
                             RedissonMultiLock.class,
                             (mock, context) -> {
                                 when(mock.tryLock(3, 10, TimeUnit.SECONDS))
                                         .thenReturn(true);
                                 when(mock.isHeldByCurrentThread())
                                         .thenReturn(true);
                             })) {
            assertThatThrownBy(
                    () -> subscriberService.createSubscriber(request))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessage("Tên subscriber đã tồn tại.");

            // Verify lock behavior
            RedissonMultiLock multiLock = mocked.constructed().get(0);
            verify(multiLock).tryLock(3, 10, TimeUnit.SECONDS);
            verify(multiLock, times(1)).unlock();

        }


    }

    @Test
    void createSubscriber_shouldTrimAndNormalizeNameForLock()
            throws Exception {
        request.setName("  Java Developer  ");

        when(redissonClient.getLock(
                "subscriber:name:java developer"))
                .thenReturn(nameLock);

        when(subscriberRepository.countByUserId(USER_ID)).thenReturn(0L);
        when(skillRepository.findAllById(List.of(SKILL_ID)))
                .thenReturn(List.of(skill));
        when(subscriberRepository.existsByName("  Java Developer  "))
                .thenReturn(false);
        when(subscriberRepository.save(any(Subscriber.class)))
                .thenReturn(subscriber);
        when(subscriberMapper.mapToResponse(subscriber))
                .thenReturn(mock(SubscriberResponse.class));
        try (MockedConstruction<RedissonMultiLock> mocked =
                     Mockito.mockConstruction(
                             RedissonMultiLock.class,
                             (mock, context) -> {
                                 when(mock.tryLock(3, 10, TimeUnit.SECONDS))
                                         .thenReturn(true);
                                 when(mock.isHeldByCurrentThread())
                                         .thenReturn(true);
                             })) {
            assertThatCode(
                    () -> subscriberService.createSubscriber(request))
                    .doesNotThrowAnyException();
            verify(redissonClient).getLock("subscriber:name:java developer");

            // Verify lock behavior
            RedissonMultiLock multiLock = mocked.constructed().get(0);
            verify(multiLock).tryLock(3, 10, TimeUnit.SECONDS);
            verify(multiLock, times(1)).unlock();

        }


    }

    @Test
    void createSubscriber_shouldRestoreInterruptFlagWhenInterrupted()
            throws Exception {
        try (MockedConstruction<RedissonMultiLock> mocked =
                     Mockito.mockConstruction(
                             RedissonMultiLock.class,
                             (mock, context) -> {
                                 when(mock.tryLock(3, 10, TimeUnit.SECONDS))
                                         .thenThrow(new InterruptedException("interrupted"));
                                 when(mock.isHeldByCurrentThread())
                                         .thenReturn(true);
                             })) {
            try {
                assertThatThrownBy(
                        () -> subscriberService.createSubscriber(request))
                        .isInstanceOf(RuntimeException.class)
                        .hasCauseInstanceOf(InterruptedException.class);

                assertThat(Thread.currentThread().isInterrupted()).isTrue();
                verify(subscriberRepository, never()).save(any());
            } finally {
                // Tránh ảnh hưởng các test chạy trên cùng thread.
                Thread.interrupted();
            }
        }
    }

    //    A. createSubscriber() — các trường hợp biên
    @Test
    void createSubscriber_shouldAllowFifthSubscriber() throws Exception {
        when(subscriberRepository.countByUserId(USER_ID)).thenReturn(4L);
        when(skillRepository.findAllById(List.of(SKILL_ID)))
                .thenReturn(List.of(skill));
        when(subscriberRepository.existsByName(SUBSCRIBER_NAME))
                .thenReturn(false);
        when(subscriberRepository.save(any(Subscriber.class)))
                .thenReturn(subscriber);
        when(subscriberMapper.mapToResponse(subscriber))
                .thenReturn(mock(SubscriberResponse.class));
        try (MockedConstruction<RedissonMultiLock> mocked =
                     Mockito.mockConstruction(
                             RedissonMultiLock.class,
                             (mock, context) -> {
                                 when(mock.tryLock(3, 10, TimeUnit.SECONDS))
                                         .thenReturn(true);
                                 when(mock.isHeldByCurrentThread())
                                         .thenReturn(true);
                             })) {
            assertThatCode(() -> subscriberService.createSubscriber(request))
                    .doesNotThrowAnyException();

            verify(subscriberRepository).save(any(Subscriber.class));

        }


    }

    @Test
    void createSubscriber_shouldRejectWhenAtLeastOneRequestedSkillIsMissing()
            throws Exception {
        Skill anotherSkill = new Skill();
        anotherSkill.setId(200L);

        request.setSkillIds(List.of(SKILL_ID, 200L));

        when(subscriberRepository.countByUserId(USER_ID)).thenReturn(0L);
        when(skillRepository.findAllById(List.of(SKILL_ID, 200L)))
                .thenReturn(List.of(skill));
        try (MockedConstruction<RedissonMultiLock> mocked =
                     Mockito.mockConstruction(
                             RedissonMultiLock.class,
                             (mock, context) -> {
                                 when(mock.tryLock(3, 10, TimeUnit.SECONDS))
                                         .thenReturn(true);
                                 when(mock.isHeldByCurrentThread())
                                         .thenReturn(true);
                             })) {
            assertThatThrownBy(() -> subscriberService.createSubscriber(request))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Một hoặc nhiều Skill không tồn tại.");

            verify(subscriberRepository, never()).save(any());
            verify(subscriberRepository, never()).existsByName(anyString());

        }

    }

    @Test
    void createSubscriber_shouldAllowEmptySkillListWhenBusinessRulesAllowIt()
            throws Exception {
        request.setSkillIds(List.of());
        when(subscriberRepository.countByUserId(USER_ID)).thenReturn(0L);
        when(subscriberRepository.existsByName(SUBSCRIBER_NAME))
                .thenReturn(false);
        when(subscriberRepository.save(any(Subscriber.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(subscriberMapper.mapToResponse(any(Subscriber.class)))
                .thenReturn(mock(SubscriberResponse.class));
        try (MockedConstruction<RedissonMultiLock> mocked =
                     Mockito.mockConstruction(
                             RedissonMultiLock.class,
                             (mock, context) -> {
                                 when(mock.tryLock(3, 10, TimeUnit.SECONDS))
                                         .thenReturn(true);
                                 when(mock.isHeldByCurrentThread())
                                         .thenReturn(true);
                             })) {
            assertThatCode(() -> subscriberService.createSubscriber(request))
                    .doesNotThrowAnyException();
            ArgumentCaptor<Subscriber> captor =
                    ArgumentCaptor.forClass(Subscriber.class);

            verify(subscriberRepository).save(captor.capture());
            assertThat(captor.getValue().getSubscriberSkills()).isEmpty();

            verify(skillRepository, never()).findAllById(any());

        }


    }

    @Test
    void createSubscriber_shouldHandleDuplicateSkillIdsAccordingToBusinessRule()
            throws Exception {
        request.setSkillIds(List.of(SKILL_ID, SKILL_ID));

        when(subscriberRepository.countByUserId(USER_ID)).thenReturn(0L);
        when(skillRepository.findAllById(List.of(SKILL_ID, SKILL_ID)))
                .thenReturn(List.of(skill));
        when(subscriberRepository.existsByName(SUBSCRIBER_NAME))
                .thenReturn(false);
        when(subscriberRepository.save(any(Subscriber.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(subscriberMapper.mapToResponse(any(Subscriber.class)))
                .thenReturn(mock(SubscriberResponse.class));

        try (MockedConstruction<RedissonMultiLock> mocked =
                     Mockito.mockConstruction(
                             RedissonMultiLock.class,
                             (mock, context) -> {
                                 when(mock.tryLock(3, 10, TimeUnit.SECONDS))
                                         .thenReturn(true);
                                 when(mock.isHeldByCurrentThread())
                                         .thenReturn(true);
                             })) {
            // Chính sách được chọn: loại bỏ ID trùng trước khi tạo quan hệ.
            // Test này sẽ thất bại với service hiện tại cho đến khi service
            // chuẩn hóa danh sách skillIds trước khi so sánh kích thước.
            assertThatCode(() -> subscriberService.createSubscriber(request))
                    .doesNotThrowAnyException();

            ArgumentCaptor<Subscriber> captor =
                    ArgumentCaptor.forClass(Subscriber.class);

            verify(subscriberRepository).save(captor.capture());
            assertThat(captor.getValue().getSubscriberSkills()).hasSize(1);


        }

    }

    @Test
    void createSubscriber_shouldNotCreateWhenNameLockCannotBeAcquired()
            throws Exception {

        try (MockedConstruction<RedissonMultiLock> mocked =
                     Mockito.mockConstruction(
                             RedissonMultiLock.class,
                             (mock, context) -> {
                                 when(mock.tryLock(3, 10, TimeUnit.SECONDS))
                                         .thenReturn(false);
                                 when(mock.isHeldByCurrentThread())
                                         .thenReturn(true);
                             })) {
            assertThatThrownBy(() -> subscriberService.createSubscriber(request))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessage("System is busy. Please retry.");
            verify(subscriberRepository, never()).countByUserId(anyLong());
            verify(subscriberRepository, never()).save(any());


        }


    }


    // ---------------------------------------------------------
    // updateSubscriber
    // ---------------------------------------------------------

    @Test
    void updateSubscriber_shouldUpdateSubscriberSuccessfully()
            throws Exception {
        when(subscriberRepository.findById(SUBSCRIBER_ID))
                .thenReturn(Optional.of(subscriber));
        when(skillRepository.findAllById(List.of(SKILL_ID)))
                .thenReturn(List.of(skill));
        when(subscriberRepository.existsByNameAndIdNot(SUBSCRIBER_NAME, SUBSCRIBER_ID))
                .thenReturn(false);
        when(subscriberRepository.save(subscriber)).thenReturn(subscriber);

        SubscriberResponse expected = mock(SubscriberResponse.class);
        when(subscriberMapper.mapToResponse(subscriber)).thenReturn(expected);


        try (MockedConstruction<RedissonMultiLock> mocked =
                     Mockito.mockConstruction(
                             RedissonMultiLock.class,
                             (mock, context) -> {
                                 when(mock.tryLock(3, 10, TimeUnit.SECONDS))
                                         .thenReturn(true);
                                 when(mock.isHeldByCurrentThread())
                                         .thenReturn(true);
                             })) {
            SubscriberResponse actual = subscriberService.updateSubscriber(SUBSCRIBER_ID, request);

            assertThat(actual).isSameAs(expected);
            assertThat(subscriber.getSubscriberSkills()).hasSize(1);
            assertThat(subscriber.getSubscriberSkills().get(0).getSkill())
                    .isSameAs(skill);
            assertThat(subscriber.getLastCheckedAt()).isNotNull();

            verify(subscriberRepository).flush();
            verify(subscriberRepository).save(subscriber);

            // Verify lock behavior
            RedissonMultiLock multiLock = mocked.constructed().get(0);
            verify(multiLock).tryLock(3, 10, TimeUnit.SECONDS);
            verify(multiLock, times(1)).unlock();

        }

    }

    @Test
    void updateSubscriber_shouldRejectWhenSubscriberBelongsToAnotherUser()
            throws Exception {
        subscriber.setUser(otherUser);
        when(subscriberRepository.findById(SUBSCRIBER_ID))
                .thenReturn(Optional.of(subscriber));
        try (MockedConstruction<RedissonMultiLock> mocked =
                     Mockito.mockConstruction(
                             RedissonMultiLock.class,
                             (mock, context) -> {
                                 when(mock.tryLock(3, 10, TimeUnit.SECONDS))
                                         .thenReturn(true);
                                 when(mock.isHeldByCurrentThread())
                                         .thenReturn(true);
                             })) {
            assertThatThrownBy(
                    () -> subscriberService.updateSubscriber(
                            SUBSCRIBER_ID, request))
                    .isInstanceOf(ForbiddenException.class)
                    .hasMessage("You dont permission !");

            verify(modelMapper, never()).map(any(), any());
            verify(subscriberRepository, never()).save(any());

            // Verify lock behavior
            RedissonMultiLock multiLock = mocked.constructed().get(0);
            verify(multiLock).tryLock(3, 10, TimeUnit.SECONDS);
            verify(multiLock, times(1)).unlock();

        }

    }

    @Test
    void updateSubscriber_shouldRejectWhenSubscriberDoesNotExist()
            throws Exception {
        when(subscriberRepository.findById(SUBSCRIBER_ID))
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
            assertThatThrownBy(
                    () -> subscriberService.updateSubscriber(
                            SUBSCRIBER_ID, request))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Subscriber not found with ID: " + SUBSCRIBER_ID);

            verify(subscriberRepository, never()).save(any());


            // Verify lock behavior
            RedissonMultiLock multiLock = mocked.constructed().get(0);
            verify(multiLock).tryLock(3, 10, TimeUnit.SECONDS);
            verify(multiLock, times(1)).unlock();

        }


    }

    @Test
    void updateSubscriber_shouldRejectWhenSkillDoesNotExist()
            throws Exception {
        when(subscriberRepository.findById(SUBSCRIBER_ID))
                .thenReturn(Optional.of(subscriber));
        when(skillRepository.findAllById(List.of(SKILL_ID)))
                .thenReturn(List.of());

        try (MockedConstruction<RedissonMultiLock> mocked =
                     Mockito.mockConstruction(
                             RedissonMultiLock.class,
                             (mock, context) -> {
                                 when(mock.tryLock(3, 10, TimeUnit.SECONDS))
                                         .thenReturn(true);
                                 when(mock.isHeldByCurrentThread())
                                         .thenReturn(true);
                             })) {
            assertThatThrownBy(
                    () -> subscriberService.updateSubscriber(
                            SUBSCRIBER_ID, request))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Một hoặc nhiều Skill không tồn tại.");

            verify(subscriberRepository, never()).save(any());

            // Verify lock behavior
            RedissonMultiLock multiLock = mocked.constructed().get(0);
            verify(multiLock).tryLock(3, 10, TimeUnit.SECONDS);
            verify(multiLock, times(1)).unlock();

        }


    }

    @Test
    void updateSubscriber_shouldRejectDuplicateName()
            throws Exception {
        when(subscriberRepository.findById(SUBSCRIBER_ID))
                .thenReturn(Optional.of(subscriber));
        when(skillRepository.findAllById(List.of(SKILL_ID)))
                .thenReturn(List.of(skill));
        when(subscriberRepository.existsByNameAndIdNot(SUBSCRIBER_NAME, SUBSCRIBER_ID))
                .thenReturn(true);

        try (MockedConstruction<RedissonMultiLock> mocked =
                     Mockito.mockConstruction(
                             RedissonMultiLock.class,
                             (mock, context) -> {
                                 when(mock.tryLock(3, 10, TimeUnit.SECONDS))
                                         .thenReturn(true);
                                 when(mock.isHeldByCurrentThread())
                                         .thenReturn(true);
                             })) {

//            action
            assertThatThrownBy(
                    () -> subscriberService.updateSubscriber(
                            SUBSCRIBER_ID, request))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessage("Tên subscriber đã tồn tại.");

            verify(subscriberRepository, never()).save(any());

            // Verify lock behavior
            RedissonMultiLock multiLock = mocked.constructed().get(0);
            verify(multiLock).tryLock(3, 10, TimeUnit.SECONDS);
            verify(multiLock, times(1)).unlock();

        }

    }

    @Test
    void updateSubscriber_shouldRejectWhenLockCannotBeAcquired()
            throws Exception {
        try (MockedConstruction<RedissonMultiLock> mocked =
                     Mockito.mockConstruction(
                             RedissonMultiLock.class,
                             (mock, context) -> {
                                 when(mock.tryLock(3, 10, TimeUnit.SECONDS))
                                         .thenReturn(false);
                                 when(mock.isHeldByCurrentThread())
                                         .thenReturn(false);
                             })) {

//            action
            assertThatThrownBy(
                    () -> subscriberService.updateSubscriber(
                            SUBSCRIBER_ID, request))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessage("System is busy. Please retry.");

            verify(subscriberRepository, never()).findById(anyLong());
            verify(subscriberRepository, never()).save(any());

            // Verify lock behavior
            RedissonMultiLock multiLock = mocked.constructed().get(0);
            verify(multiLock).tryLock(3, 10, TimeUnit.SECONDS);
            verify(multiLock, never()).unlock();

        }
    }

    @Test
    void updateSubscriber_shouldTranslateDataIntegrityViolation()
            throws Exception {
        when(subscriberRepository.findById(SUBSCRIBER_ID))
                .thenReturn(Optional.of(subscriber));
        when(skillRepository.findAllById(List.of(SKILL_ID)))
                .thenReturn(List.of(skill));
        when(subscriberRepository.existsByNameAndIdNot(SUBSCRIBER_NAME, SUBSCRIBER_ID))
                .thenReturn(false);
        when(subscriberRepository.save(subscriber))
                .thenThrow(new DataIntegrityViolationException("duplicate"));

        try (MockedConstruction<RedissonMultiLock> mocked =
                     Mockito.mockConstruction(
                             RedissonMultiLock.class,
                             (mock, context) -> {
                                 when(mock.tryLock(3, 10, TimeUnit.SECONDS))
                                         .thenReturn(true);
                                 when(mock.isHeldByCurrentThread())
                                         .thenReturn(true);
                             })) {

//            act
            assertThatThrownBy(
                    () -> subscriberService.updateSubscriber(
                            SUBSCRIBER_ID, request))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessage("Tên subscriber đã tồn tại.");

            // Verify lock behavior
            RedissonMultiLock multiLock = mocked.constructed().get(0);
            verify(multiLock).tryLock(3, 10, TimeUnit.SECONDS);
            verify(multiLock, times(1)).unlock();

        }
    }

    @Test
    void updateSubscriber_shouldRestoreInterruptFlagWhenInterrupted()
            throws Exception {
        try (MockedConstruction<RedissonMultiLock> mocked =
                     Mockito.mockConstruction(
                             RedissonMultiLock.class,
                             (mock, context) -> {
                                 when(mock.tryLock(3, 10, TimeUnit.SECONDS))
                                         .thenThrow(new InterruptedException("interrupted"));
                                 when(mock.isHeldByCurrentThread())
                                         .thenReturn(true);
                             })) {

//            act
            try {
                assertThatThrownBy(
                        () -> subscriberService.updateSubscriber(
                                SUBSCRIBER_ID, request))
                        .isInstanceOf(RuntimeException.class)
                        .hasCauseInstanceOf(InterruptedException.class);
                assertThat(Thread.currentThread().isInterrupted()).isTrue();
                verify(subscriberRepository, never()).save(any());

            } finally {
                Thread.interrupted();
            }

        }
    }

    @Test
    void updateSubscriber_shouldAllowKeepingItsCurrentName()
            throws Exception {
        when(subscriberRepository.findById(SUBSCRIBER_ID))
                .thenReturn(Optional.of(subscriber));
        when(skillRepository.findAllById(List.of(SKILL_ID)))
                .thenReturn(List.of(skill));

        // Giả định service đã được sửa để loại trừ subscriber hiện tại
        // khi kiểm tra tên trùng.
        when(subscriberRepository.existsByNameAndIdNot(
                SUBSCRIBER_NAME, SUBSCRIBER_ID))
                .thenReturn(false);

        when(subscriberRepository.save(subscriber)).thenReturn(subscriber);
        when(subscriberMapper.mapToResponse(subscriber))
                .thenReturn(mock(SubscriberResponse.class));
        try (MockedConstruction<RedissonMultiLock> mocked =
                     Mockito.mockConstruction(
                             RedissonMultiLock.class,
                             (mock, context) -> {
                                 when(mock.tryLock(3, 10, TimeUnit.SECONDS))
                                         .thenReturn(true);
                                 when(mock.isHeldByCurrentThread())
                                         .thenReturn(true);
                             })) {

//            act
            assertThatCode(() ->
                    subscriberService.updateSubscriber(SUBSCRIBER_ID, request))
                    .doesNotThrowAnyException();

            verify(subscriberRepository).save(subscriber);


        }

    }

    @Test
    void updateSubscriber_shouldRejectNameOwnedByAnotherSubscriber()
            throws Exception {
        when(subscriberRepository.findById(SUBSCRIBER_ID))
                .thenReturn(Optional.of(subscriber));
        when(skillRepository.findAllById(List.of(SKILL_ID)))
                .thenReturn(List.of(skill));
        when(subscriberRepository.existsByNameAndIdNot(
                SUBSCRIBER_NAME, SUBSCRIBER_ID))
                .thenReturn(true);

        try (MockedConstruction<RedissonMultiLock> mocked =
                     Mockito.mockConstruction(
                             RedissonMultiLock.class,
                             (mock, context) -> {
                                 when(mock.tryLock(3, 10, TimeUnit.SECONDS))
                                         .thenReturn(true);
                                 when(mock.isHeldByCurrentThread())
                                         .thenReturn(true);
                             })) {

//            act
            assertThatThrownBy(() ->
                    subscriberService.updateSubscriber(SUBSCRIBER_ID, request))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessage("Tên subscriber đã tồn tại.");

            verify(subscriberRepository, never()).save(any());

        }
    }

    @Test
    void updateSubscriber_shouldTranslateIntegrityViolationFromFlush()
            throws Exception {
        when(subscriberRepository.findById(SUBSCRIBER_ID))
                .thenReturn(Optional.of(subscriber));
        when(skillRepository.findAllById(List.of(SKILL_ID)))
                .thenReturn(List.of(skill));

        // Code hiện tại vẫn dùng existsByName.
        when(subscriberRepository.existsByNameAndIdNot(SUBSCRIBER_NAME,SUBSCRIBER_ID))
                .thenReturn(false);

        doThrow(new DataIntegrityViolationException("constraint violation"))
                .when(subscriberRepository).flush();
        try (MockedConstruction<RedissonMultiLock> mocked =
                     Mockito.mockConstruction(
                             RedissonMultiLock.class,
                             (mock, context) -> {
                                 when(mock.tryLock(3, 10, TimeUnit.SECONDS))
                                         .thenReturn(true);
                                 when(mock.isHeldByCurrentThread())
                                         .thenReturn(true);
                             })) {

//            act
            assertThatThrownBy(() ->
                    subscriberService.updateSubscriber(SUBSCRIBER_ID, request))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessage("Tên subscriber đã tồn tại.");

            verify(subscriberRepository, never()).save(any());

        }

    }

    @Test
    void updateSubscriber_shouldClearSkillsWhenEmptySkillListIsAllowed()
            throws Exception {
        request.setSkillIds(List.of());

        when(subscriberRepository.findById(SUBSCRIBER_ID))
                .thenReturn(Optional.of(subscriber));
        when(subscriberRepository.existsByNameAndIdNot(SUBSCRIBER_NAME,SUBSCRIBER_ID))
                .thenReturn(false);
        when(subscriberRepository.save(subscriber)).thenReturn(subscriber);
        when(subscriberMapper.mapToResponse(subscriber))
                .thenReturn(mock(SubscriberResponse.class));
        try (MockedConstruction<RedissonMultiLock> mocked =
                     Mockito.mockConstruction(
                             RedissonMultiLock.class,
                             (mock, context) -> {
                                 when(mock.tryLock(3, 10, TimeUnit.SECONDS))
                                         .thenReturn(true);
                                 when(mock.isHeldByCurrentThread())
                                         .thenReturn(true);
                             })) {
            subscriberService.updateSubscriber(SUBSCRIBER_ID, request);

            assertThat(subscriber.getSubscriberSkills()).isEmpty();

            verify(subscriberRepository).flush();
            verify(subscriberRepository).save(subscriber);
            verify(skillRepository, never()).findAllById(any());

        }

    }




    // ---------------------------------------------------------
    // getById
    // ---------------------------------------------------------

    @Test
    void getById_shouldReturnSubscriberWhenFound() {
        when(subscriberRepository.findById(SUBSCRIBER_ID))
                .thenReturn(Optional.of(subscriber));

        assertThat(subscriberService.getById(SUBSCRIBER_ID))
                .isSameAs(subscriber);
    }

    @Test
    void getById_shouldThrowWhenSubscriberNotFound() {
        when(subscriberRepository.findById(SUBSCRIBER_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> subscriberService.getById(SUBSCRIBER_ID))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Subscriber not found with ID: " + SUBSCRIBER_ID);
    }

    // ---------------------------------------------------------
    // deleteSubscriber
    // ---------------------------------------------------------

    @Test
    void deleteSubscriber_shouldClearSkillsAndDeleteSubscriber() {
        SubscriberSkill subscriberSkill = new SubscriberSkill();
        subscriberSkill.setSubscriber(subscriber);
        subscriberSkill.setSkill(skill);
        subscriber.getSubscriberSkills().add(subscriberSkill);

        when(subscriberRepository.findById(SUBSCRIBER_ID))
                .thenReturn(Optional.of(subscriber));

        subscriberService.deleteSubscriber(SUBSCRIBER_ID);

        assertThat(subscriber.getSubscriberSkills()).isEmpty();

        var order = inOrder(subscriberRepository);
        order.verify(subscriberRepository).flush();
        order.verify(subscriberRepository).delete(subscriber);
    }

    @Test
    void deleteSubscriber_shouldRejectWhenUserDoesNotOwnSubscriber() {
        subscriber.setUser(otherUser);

        when(subscriberRepository.findById(SUBSCRIBER_ID))
                .thenReturn(Optional.of(subscriber));

        assertThatThrownBy(
                () -> subscriberService.deleteSubscriber(SUBSCRIBER_ID))
                .isInstanceOf(ForbiddenException.class)
                .hasMessage("You dont permission !");

        verify(subscriberRepository, never()).flush();
        verify(subscriberRepository, never()).delete(any(Subscriber.class));
    }

    @Test
    void deleteSubscriber_shouldThrowWhenSubscriberNotFound() {
        when(subscriberRepository.findById(SUBSCRIBER_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(
                () -> subscriberService.deleteSubscriber(SUBSCRIBER_ID))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(subscriberRepository, never()).delete(any(Subscriber.class));
    }

    @Test
    void deleteSubscriber_shouldNotDeleteWhenFlushFails() {
        when(subscriberRepository.findById(SUBSCRIBER_ID))
                .thenReturn(Optional.of(subscriber));

        doThrow(new DataIntegrityViolationException("flush failed"))
                .when(subscriberRepository).flush();

        assertThatThrownBy(() ->
                subscriberService.deleteSubscriber(SUBSCRIBER_ID))
                .isInstanceOf(DataIntegrityViolationException.class);

        verify(subscriberRepository).flush();
        verify(subscriberRepository, never()).delete(subscriber);
    }


    // ---------------------------------------------------------
    // getDetail
    // ---------------------------------------------------------

    @Test
    void getDetail_shouldReturnMappedResponse() {
        SubscriberResponse expected = mock(SubscriberResponse.class);

        when(subscriberRepository.findById(SUBSCRIBER_ID))
                .thenReturn(Optional.of(subscriber));
        when(subscriberMapper.mapToResponse(subscriber))
                .thenReturn(expected);

        assertThat(subscriberService.getDetail(SUBSCRIBER_ID))
                .isSameAs(expected);

        verify(subscriberMapper).mapToResponse(subscriber);
    }

    @Test
    void getDetail_shouldThrowWhenSubscriberNotFound() {
        when(subscriberRepository.findById(SUBSCRIBER_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(
                () -> subscriberService.getDetail(SUBSCRIBER_ID))
                .isInstanceOf(ResourceNotFoundException.class);

        verifyNoInteractions(subscriberMapper);
    }

    // ---------------------------------------------------------
    // getAllSubscriber
    // ---------------------------------------------------------

    @Test
    void getAllSubscriber_shouldReturnMappedPage() {
        SubscriberFilterRequest filter = new SubscriberFilterRequest();
        SubscriberListResponse item = mock(SubscriberListResponse.class);

        Page<Subscriber> page = new PageImpl<>(
                List.of(subscriber),
                PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt")),
                1
        );

        when(subscriberRepository.findAll(
                any(org.springframework.data.jpa.domain.Specification.class),
                any(Pageable.class)))
                .thenReturn(page);
        when(subscriberMapper.toResponseSubscriberList(subscriber))
                .thenReturn(item);

        ResponseDTO<List<SubscriberListResponse>> result =
                subscriberService.getAllSubscriber(1, 10, filter);

        assertThat(result.getResult()).containsExactly(item);
        assertThat(result.getMeta()).isNotNull();

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);

        verify(subscriberRepository).findAll(
                any(org.springframework.data.jpa.domain.Specification.class),
                captor.capture());

        Pageable pageable = captor.getValue();

        assertThat(pageable.getPageNumber()).isZero();
        assertThat(pageable.getPageSize()).isEqualTo(10);
        assertThat(pageable.getSort().getOrderFor("createdAt").getDirection())
                .isEqualTo(Sort.Direction.DESC);
    }

    @Test
    void getAllSubscriber_shouldAcceptNullFilter() {
        when(subscriberRepository.findAll(
                any(org.springframework.data.jpa.domain.Specification.class),
                any(Pageable.class)))
                .thenReturn(Page.empty());

        assertThatCode(
                () -> subscriberService.getAllSubscriber(1, 10, null))
                .doesNotThrowAnyException();

        verify(subscriberRepository).findAll(
                any(org.springframework.data.jpa.domain.Specification.class),
                any(Pageable.class));
    }

    @Test
    void getAllSubscriber_shouldNormalizePageAndSize() {
        when(subscriberRepository.findAll(
                any(org.springframework.data.jpa.domain.Specification.class),
                any(Pageable.class)))
                .thenReturn(Page.empty());

        subscriberService.getAllSubscriber(-5, 0, new SubscriberFilterRequest());

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);

        verify(subscriberRepository).findAll(
                any(org.springframework.data.jpa.domain.Specification.class),
                captor.capture());

        assertThat(captor.getValue().getPageNumber()).isZero();
        assertThat(captor.getValue().getPageSize()).isEqualTo(1);
    }

    @Test
    void getAllSubscriber_shouldLimitPageSizeToFifty() {
        when(subscriberRepository.findAll(
                any(org.springframework.data.jpa.domain.Specification.class),
                any(Pageable.class)))
                .thenReturn(Page.empty());

        subscriberService.getAllSubscriber(2, 100, new SubscriberFilterRequest());

        ArgumentCaptor<Pageable> captor =
                ArgumentCaptor.forClass(Pageable.class);

        verify(subscriberRepository).findAll(
                any(org.springframework.data.jpa.domain.Specification.class),
                captor.capture());

        assertThat(captor.getValue().getPageNumber()).isEqualTo(1);
        assertThat(captor.getValue().getPageSize()).isEqualTo(50);
    }

    @Test
    void getAllSubscriber_shouldReturnEmptyListWhenPageIsEmpty() {
        when(subscriberRepository.findAll(
                any(org.springframework.data.jpa.domain.Specification.class),
                any(Pageable.class)))
                .thenReturn(Page.empty());

        ResponseDTO<List<SubscriberListResponse>> result =
                subscriberService.getAllSubscriber(
                        1, 10, new SubscriberFilterRequest());

        assertThat(result.getResult()).isEmpty();
    }

    @Test
    void getAllSubscriber_shouldReturnCorrectPaginationMetadata() {
        SubscriberFilterRequest filter = new SubscriberFilterRequest();

        Page<Subscriber> page = new PageImpl<>(
                List.of(subscriber),
                PageRequest.of(
                        1, 10,
                        Sort.by(Sort.Direction.DESC, "createdAt")),
                25
        );

        when(subscriberRepository.findAll(
                any(org.springframework.data.jpa.domain.Specification.class),
                any(Pageable.class)))
                .thenReturn(page);

        when(subscriberMapper.toResponseSubscriberList(subscriber))
                .thenReturn(mock(SubscriberListResponse.class));

        ResponseDTO<List<SubscriberListResponse>> result =
                subscriberService.getAllSubscriber(2, 10, filter);

        assertThat(result.getMeta().getCurrent()).isEqualTo(2);
        assertThat(result.getMeta().getPageSize()).isEqualTo(10);
        assertThat(result.getMeta().getTotalPages()).isEqualTo(3);
        assertThat(result.getMeta().getTotalItems()).isEqualTo(25L);

        assertThat(result.getResult()).hasSize(1);
    }


    // ---------------------------------------------------------
    // updateSubscription
    // ---------------------------------------------------------

    @Test
    void updateSubscription_shouldSetSubscribedTrue() {
        when(subscriberRepository.findById(SUBSCRIBER_ID))
                .thenReturn(Optional.of(subscriber));

        subscriberService.updateSubscription(SUBSCRIBER_ID, true);

        assertThat(subscriber.isSubscribed()).isTrue();
        verify(subscriberRepository, never()).save(any());
    }

    @Test
    void updateSubscription_shouldSetSubscribedFalse() {
        subscriber.setSubscribed(true);

        when(subscriberRepository.findById(SUBSCRIBER_ID))
                .thenReturn(Optional.of(subscriber));

        subscriberService.updateSubscription(SUBSCRIBER_ID, false);

        assertThat(subscriber.isSubscribed()).isFalse();
    }

    @Test
    void updateSubscription_shouldRejectWhenUserDoesNotOwnSubscriber() {
        subscriber.setUser(otherUser);


        when(subscriberRepository.findById(SUBSCRIBER_ID))
                .thenReturn(Optional.of(subscriber));

        assertThatThrownBy(
                () -> subscriberService.updateSubscription(
                        SUBSCRIBER_ID, true))
                .isInstanceOf(ForbiddenException.class)
                .hasMessage("You don't have permission!");


    }

    @Test
    void updateSubscription_shouldThrowWhenSubscriberNotFound() {
        when(subscriberRepository.findById(SUBSCRIBER_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(
                () -> subscriberService.updateSubscription(
                        SUBSCRIBER_ID, true))
                .isInstanceOf(ResourceNotFoundException.class);
    }


}




