package com.webjob.application.service;

import com.webjob.application.dto.Response.ResponseDTO;
import com.webjob.application.dto.record.PermissionCacheInvalidationEvent;
import com.webjob.application.models.Entity.Permission;
import com.webjob.application.repository.PermissionRepository;
import com.webjob.application.repository.RolePermissionRepository;
import com.webjob.application.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PermissionService Unit Tests")
public class PermissionServiceTest {
    @Mock
    private PermissionRepository permissionRepository;

    @Mock
    private RolePermissionRepository rolePermissionRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private UserRepository userRepository;

    @Spy
    @InjectMocks
    private PermissionService permissionService;

    private Permission permission;

    @BeforeEach
    void setUp() {
        permission = new Permission();
        permission.setId(1L);
        permission.setName("View users");
        permission.setApiPath("/api/users");
        permission.setMethod("GET");
        permission.setModule("USER");
        permission.setCode("USER_VIEW");
    }

    // ---------------------------------------------------------
    // savePermission
    // ---------------------------------------------------------

    @Test
    void savePermission_whenPermissionDoesNotExist_shouldSavePermission() {
        when(permissionRepository.existsByApiPathAndMethodAndModule(
                "/api/users", "GET", "USER"
        )).thenReturn(false);

        when(permissionRepository.save(permission)).thenReturn(permission);

        Permission result = permissionService.savePermission(permission);

        assertSame(permission, result);

        verify(permissionRepository).existsByApiPathAndMethodAndModule(
                "/api/users", "GET", "USER"
        );
        verify(permissionRepository).save(permission);
    }

    @Test
    void savePermission_whenPermissionAlreadyExists_shouldThrowException() {
        when(permissionRepository.existsByApiPathAndMethodAndModule(
                "/api/users", "GET", "USER"
        )).thenReturn(true);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> permissionService.savePermission(permission)
        );

        assertEquals(
                "Permission already exists :USER_VIEW",
                exception.getMessage()
        );

        verify(permissionRepository, never()).save(any(Permission.class));
    }

    // ---------------------------------------------------------
    // createPermission
    // ---------------------------------------------------------

    @Test
    void createPermission_shouldDelegateToSavePermission() {
        doReturn(permission)
                .when(permissionService).savePermission(permission);

        Permission result = permissionService.createPermission(permission);

        assertSame(permission, result);
        assertEquals("/api/users",result.getApiPath());
        assertEquals("GET",result.getMethod());
        verify(permissionService).savePermission(permission);
    }
    // ---------------------------------------------------------
    // getByID
    // ---------------------------------------------------------

    @Test
    void getByID_whenPermissionExists_shouldReturnPermission() {
        when(permissionRepository.findById(1L))
                .thenReturn(Optional.of(permission));

        Permission result = permissionService.getByID(1L);

        assertSame(permission, result);
    }

    @Test
    void getByID_whenPermissionDoesNotExist_shouldThrowException() {
        when(permissionRepository.findById(99L))
                .thenReturn(Optional.empty());

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> permissionService.getByID(99L)
        );

        assertEquals(
                "Permission not found with 99",
                exception.getMessage()
        );
    }

    // ---------------------------------------------------------
    // editPermission
    // ---------------------------------------------------------

    @Test
    void editPermission_whenPermissionExists_shouldUpdateAndPublishEvent() {
        Permission updated = new Permission();
        updated.setName("Create users");
        updated.setApiPath("/api/users");
        updated.setMethod("POST");
        updated.setModule("USER");
        updated.setCode("USER_CREATE");

        List<Long> userIds = List.of(10L, 20L);

        when(permissionRepository.findById(1L))
                .thenReturn(Optional.of(permission));
        when(userRepository.findUserIdsByPermissionId(1L))
                .thenReturn(userIds);
        when(permissionRepository.save(any(Permission.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Permission result = permissionService.editPermission(1L, updated);

        assertSame(permission, result);
        assertEquals("Create users", result.getName());
        assertEquals("/api/users", result.getApiPath());
        assertEquals("POST", result.getMethod());
        assertEquals("USER", result.getModule());
        assertEquals("USER_CREATE", result.getCode());

        verify(userRepository).findUserIdsByPermissionId(1L);
        verify(permissionRepository).save(permission);
        verify(eventPublisher).publishEvent(
                any(PermissionCacheInvalidationEvent.class)
        );
    }

    @Test
    void editPermission_whenPermissionDoesNotExist_shouldThrowException() {
        when(permissionRepository.findById(99L))
                .thenReturn(Optional.empty());

        assertThrows(
                IllegalArgumentException.class,
                () -> permissionService.editPermission(99L, permission)
        );

        verify(userRepository, never()).findUserIdsByPermissionId(anyLong());
        verify(permissionRepository, never()).save(any(Permission.class));
        verify(eventPublisher, never()).publishEvent(any());
    }


    // ---------------------------------------------------------
    // deletePerMission
    // ---------------------------------------------------------

    @Test
    void deletePerMission_whenPermissionExists_shouldDeleteRelationsAndPublishEvent() {
        List<Long> userIds = List.of(10L, 20L);

        when(permissionRepository.findById(1L))
                .thenReturn(Optional.of(permission));
        when(userRepository.findUserIdsByPermissionId(1L))
                .thenReturn(userIds);

        permissionService.deletePerMission(1L);

        InOrder inOrder = inOrder(
                rolePermissionRepository,
                eventPublisher,
                permissionRepository
        );

        inOrder.verify(rolePermissionRepository).deleteByPermissionId(1L);
        inOrder.verify(eventPublisher).publishEvent(
                any(PermissionCacheInvalidationEvent.class)
        );
        inOrder.verify(permissionRepository).delete(permission);
    }

    @Test
    void deletePerMission_whenPermissionDoesNotExist_shouldNotDeleteAnything() {
        when(permissionRepository.findById(99L))
                .thenReturn(Optional.empty());

        assertThrows(
                IllegalArgumentException.class,
                () -> permissionService.deletePerMission(99L)
        );

        verify(userRepository, never()).findUserIdsByPermissionId(anyLong());
        verify(rolePermissionRepository, never()).deleteByPermissionId(anyLong());
        verify(eventPublisher, never()).publishEvent(any());
        verify(permissionRepository, never()).delete(any(Permission.class));
    }

    // ---------------------------------------------------------
    // getAllPage
    // ---------------------------------------------------------

    @Test
    void getAllPage_shouldUseRequestedPageSizeAndAscendingIdSort() {
        Page<Permission> expectedPage = new PageImpl<>(List.of(permission));

        when(permissionRepository.findAll(any(Pageable.class)))
                .thenReturn(expectedPage);

        Page<Permission> result = permissionService.getAllPage(2, 10);

        assertSame(expectedPage, result);

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);

        verify(permissionRepository).findAll(pageableCaptor.capture());

        Pageable pageable = pageableCaptor.getValue();

        assertEquals(2, pageable.getPageNumber());
        assertEquals(10, pageable.getPageSize());
        assertEquals(
                Sort.by(Sort.Direction.ASC, "id"),
                pageable.getSort()
        );
    }


// ---------------------------------------------------------
    // getPaginated
    // ---------------------------------------------------------

    @Test
    void getPaginated_shouldReturnContentAndCorrectMetadata() {
        Permission secondPermission = new Permission();
        secondPermission.setId(2L);

        Page<Permission> page = new PageImpl<>(
                List.of(permission, secondPermission),
                PageRequest.of(1, 2, Sort.by(Sort.Direction.ASC, "id")),
                5
        );

        when(permissionRepository.findAll(any(Pageable.class)))
                .thenReturn(page);

        ResponseDTO<List<Permission>> result = permissionService.getPaginated(2, 2);

        assertNotNull(result);

        // Điều chỉnh tên getter theo ResponseDTO/MetaDTO của dự án.
        assertEquals(2, result.getResult().size());
        assertSame(permission, result.getResult().get(0));
        assertSame(secondPermission, result.getResult().get(1));

        assertEquals(2, result.getMeta().getCurrent());
        assertEquals(2, result.getMeta().getPageSize());
        assertEquals(3, result.getMeta().getTotalPages());
        assertEquals(5L, result.getMeta().getTotalItems());
    }

    @Test
    void getPaginated_shouldNormalizePageAndSizeToAllowedBounds() {
        when(permissionRepository.findAll(any(Pageable.class)))
                .thenReturn(Page.empty());

        permissionService.getPaginated(0, 0);

        ArgumentCaptor<Pageable> pageableCaptor =
                ArgumentCaptor.forClass(Pageable.class);

        verify(permissionRepository).findAll(pageableCaptor.capture());

        Pageable pageable = pageableCaptor.getValue();

        // page được chuẩn hóa về 1, rồi chuyển thành index 0.
        assertEquals(0, pageable.getPageNumber());
        assertEquals(1, pageable.getPageSize());
    }

    @Test
    void getPaginated_whenSizeExceedsMaximum_shouldLimitSizeTo50() {
        when(permissionRepository.findAll(any(Pageable.class)))
                .thenReturn(Page.empty());

        permissionService.getPaginated(1, 100);

        ArgumentCaptor<Pageable> pageableCaptor =
                ArgumentCaptor.forClass(Pageable.class);

        verify(permissionRepository).findAll(pageableCaptor.capture());

        assertEquals(50, pageableCaptor.getValue().getPageSize());
        assertEquals(0, pageableCaptor.getValue().getPageNumber());
    }

//    Xác minh user ID trong sự kiện khi cập nhật
    @Test
    void editPermission_shouldPublishEventWithAffectedUserIds() {
        List<Long> userIds = List.of(10L, 20L);

        when(permissionRepository.findById(1L))
                .thenReturn(Optional.of(permission));
        when(userRepository.findUserIdsByPermissionId(1L))
                .thenReturn(userIds);
        when(permissionRepository.save(any(Permission.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        permissionService.editPermission(1L, permission);

        ArgumentCaptor<PermissionCacheInvalidationEvent> captor = ArgumentCaptor.forClass(PermissionCacheInvalidationEvent.class);

        verify(eventPublisher).publishEvent(captor.capture());

        assertEquals(userIds, captor.getValue().userIds());
    }

//    Xác minh user ID trong sự kiện khi xóa

    @Test
    void deletePerMission_shouldPublishEventWithAffectedUserIds() {
        List<Long> userIds = List.of(10L, 20L);

        when(permissionRepository.findById(1L))
                .thenReturn(Optional.of(permission));
        when(userRepository.findUserIdsByPermissionId(1L))
                .thenReturn(userIds);

        permissionService.deletePerMission(1L);

        ArgumentCaptor<PermissionCacheInvalidationEvent> captor = ArgumentCaptor.forClass(PermissionCacheInvalidationEvent.class);

        verify(eventPublisher).publishEvent(captor.capture());

        assertEquals(userIds, captor.getValue().userIds());
    }


//    Kiểm tra page âm và size vượt giới hạn cùng lúc
    @Test
    void getPaginated_whenPageIsNegativeAndSizeIsTooLarge_shouldNormalizeArguments() {
        when(permissionRepository.findAll(any(Pageable.class)))
                .thenReturn(Page.empty());

        permissionService.getPaginated(-5, 100);

        ArgumentCaptor<Pageable> captor =
                ArgumentCaptor.forClass(Pageable.class);

        verify(permissionRepository).findAll(captor.capture());

        Pageable pageable = captor.getValue();

        assertEquals(0, pageable.getPageNumber());
        assertEquals(50, pageable.getPageSize());
    }


}

