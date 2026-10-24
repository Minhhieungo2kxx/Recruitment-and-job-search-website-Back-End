package com.webjob.application.service;

import com.webjob.application.dto.Request.RoleRequest;
import com.webjob.application.dto.Response.ResponseDTO;
import com.webjob.application.dto.Response.RoleResponse;
import com.webjob.application.dto.record.PermissionCacheInvalidationEvent;
import com.webjob.application.mapper.RoleMapper;
import com.webjob.application.models.Entity.Permission;
import com.webjob.application.models.Entity.Role;
import com.webjob.application.models.Entity.RolePermission;
import com.webjob.application.repository.PermissionRepository;
import com.webjob.application.repository.RolePermissionRepository;
import com.webjob.application.repository.RoleRepository;
import com.webjob.application.repository.UserRepository;
import com.webjob.application.service.Redis.PermissionCacheService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.stream.StreamSupport;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PermissionService Unit Tests")
public class RoleServiceTest {
    @Mock
    private RoleRepository roleRepository;

    @Mock
    private PermissionRepository permissionRepository;

    @Mock
    private PermissionCacheService permissionCacheService;

    @Mock
    private ModelMapper modelMapper;

    @Mock
    private RolePermissionRepository rolePermissionRepository;

    @Mock
    private RoleMapper roleMapper;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private RoleService roleService;

    private Role role;
    private RoleRequest request;
    private RoleResponse response;

    @BeforeEach
    void setUp() {
        role = new Role();
        role.setId(1L);
        request = mock(RoleRequest.class);
        response = mock(RoleResponse.class);
    }

    // CREATE ROLE
    // ---------------------------------------------------------

    @Test
    void createRole_shouldCreateRoleWithPermissions() {
        Permission permission1 = new Permission();
        Permission permission2 = new Permission();

        when(request.getCode()).thenReturn("ADMIN");
        when(request.getPermissionIds()).thenReturn(List.of(1L, 2L));
        when(roleRepository.existsByCodeAndActiveTrue("ADMIN"))
                .thenReturn(false);
        when(roleRepository.save(any(Role.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(permissionRepository.findByIdIn(List.of(1L, 2L)))
                .thenReturn(List.of(permission1, permission2));
        when(roleMapper.toResponse(any(Role.class))).thenReturn(response);

        RoleResponse result = roleService.createRole(request);

        assertThat(result).isSameAs(response);

        verify(roleRepository).save(any(Role.class));

        verify(rolePermissionRepository).saveAll(argThat(items -> {
            List<RolePermission> savedItems =
                    StreamSupport.stream(items.spliterator(), false).toList();

            return savedItems.size() == 2
                    && savedItems.stream().allMatch(rp ->
                    rp.getRole() != null
                            && rp.getPermission() != null);
        }));
        verify(roleMapper).toResponse(any(Role.class));
    }


    @Test
    void createRole_shouldRejectDuplicateActiveCode() {
        when(request.getCode()).thenReturn("ADMIN");
        when(roleRepository.existsByCodeAndActiveTrue("ADMIN"))
                .thenReturn(true);

        assertThatThrownBy(() -> roleService.createRole(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Role đã tồn tại: ADMIN");

        verify(roleRepository, never()).save(any(Role.class));
        verifyNoInteractions(permissionRepository, rolePermissionRepository);
    }

    @Test
    void createRole_shouldCreateRoleWithoutPermissions() {
        when(request.getCode()).thenReturn("VIEWER");
        when(request.getPermissionIds()).thenReturn(List.of());
        when(roleRepository.existsByCodeAndActiveTrue("VIEWER"))
                .thenReturn(false);
        when(roleRepository.save(any(Role.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(permissionRepository.findByIdIn(List.of()))
                .thenReturn(List.of());
        when(roleMapper.toResponse(any(Role.class))).thenReturn(response);

        RoleResponse result = roleService.createRole(request);

        assertThat(result).isSameAs(response);

        verify(rolePermissionRepository, never()).saveAll(anyList());
        verify(roleMapper).toResponse(any(Role.class));
    }

    @Test
    void createRole_shouldUseOnlyPermissionsReturnedByRepository() {
        Permission validPermission = new Permission();

        when(request.getCode()).thenReturn("EDITOR");
        when(request.getPermissionIds()).thenReturn(List.of(1L, 999L));
        when(roleRepository.existsByCodeAndActiveTrue("EDITOR"))
                .thenReturn(false);
        when(roleRepository.save(any(Role.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(permissionRepository.findByIdIn(List.of(1L, 999L)))
                .thenReturn(List.of(validPermission));
        when(roleMapper.toResponse(any(Role.class))).thenReturn(response);

        roleService.createRole(request);

        verify(rolePermissionRepository).saveAll(argThat(items -> {
            List<RolePermission> savedItems = StreamSupport.stream(items.spliterator(), false).toList();

            return savedItems.size() == 1
                    && savedItems.get(0).getPermission() == validPermission;
        }));
    }

    // ---------------------------------------------------------
    // EDIT ROLE
    // ---------------------------------------------------------

    @Test
    void editRole_shouldUpdateRoleAndInvalidateUserPermissionCache() {
        Permission permission = new Permission();
        List<Long> userIds = List.of(10L, 20L);

        when(roleRepository.findByIdAndActiveTrue(1L))
                .thenReturn(Optional.of(role));
        when(userRepository.findUserIdsByRoleId(1L))
                .thenReturn(userIds);
        when(request.getPermissionIds()).thenReturn(List.of(5L));
        when(permissionRepository.findByIdIn(List.of(5L)))
                .thenReturn(List.of(permission));
        when(roleRepository.save(role)).thenReturn(role);
        when(roleMapper.toResponse(role)).thenReturn(response);

        RoleResponse result = roleService.editRole(1L, request);

        assertThat(result).isSameAs(response);

        verify(rolePermissionRepository).deleteByRoleId(1L);
        verify(rolePermissionRepository, atLeastOnce()).saveAll(anyList());
        verify(roleRepository).save(role);
        verify(eventPublisher).publishEvent(any(
                PermissionCacheInvalidationEvent.class));
    }

    @Test
    void editRole_shouldRejectWhenRoleDoesNotExist() {
        when(roleRepository.findByIdAndActiveTrue(99L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> roleService.editRole(99L, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Role not found");

        verifyNoInteractions(userRepository, permissionRepository,
                rolePermissionRepository, eventPublisher);
        verify(roleRepository, never()).save(any(Role.class));
    }

    // ---------------------------------------------------------
    // PAGINATION
    // ---------------------------------------------------------
    @Test
    void getAllPage_shouldReturnPageSortedByIdAscending() {
        Page<Role> expectedPage = new PageImpl<>(
                List.of(role),
                PageRequest.of(0, 10, Sort.by(Sort.Direction.ASC, "id")),
                1
        );

        when(roleRepository.findAll(any(Pageable.class)))
                .thenReturn(expectedPage);

        Page<Role> result = roleService.getAllPage(0, 10);

        assertThat(result).isSameAs(expectedPage);

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(roleRepository).findAll(captor.capture());

        assertThat(captor.getValue().getPageNumber()).isZero();
        assertThat(captor.getValue().getPageSize()).isEqualTo(10);
        assertThat(captor.getValue().getSort())
                .isEqualTo(Sort.by(Sort.Direction.ASC, "id"));
    }

    @Test
    void getPaginated_shouldReturnMappedPageAndMetadata() {
        Role secondRole = new Role();
        Role threeRole = new Role();
        Page<Role> page = new PageImpl<>(
                List.of(role, secondRole),
                PageRequest.of(0, 2, Sort.by("id").ascending()),
                5
        );

        RoleResponse secondResponse = mock(RoleResponse.class);

        when(roleRepository.findAll(any(Pageable.class))).thenReturn(page);
        when(roleMapper.toResponse(role)).thenReturn(response);
        when(roleMapper.toResponse(secondRole)).thenReturn(secondResponse);

        ResponseDTO<List<RoleResponse>> result =
                roleService.getPaginated(1, 2);

        assertThat(result).isNotNull();
        assertThat(result.getResult())
                .containsExactly(response, secondResponse);

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(roleRepository).findAll(captor.capture());

        assertThat(captor.getValue().getPageNumber()).isZero();
        assertThat(captor.getValue().getPageSize()).isEqualTo(2);
    }

    @Test
    void getPaginated_shouldNormalizeInvalidPageAndSize() {
        when(roleRepository.findAll(any(Pageable.class)))
                .thenReturn(Page.empty(
                        PageRequest.of(0, 10, Sort.by("id").ascending())));

        roleService.getPaginated(0, -1);

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(roleRepository).findAll(captor.capture());

        assertThat(captor.getValue().getPageNumber()).isZero();
        assertThat(captor.getValue().getPageSize()).isEqualTo(1);
    }


    // ---------------------------------------------------------
    // FIND ROLE
    // ---------------------------------------------------------

    @Test
    void getByid_shouldReturnActiveRoleWhenFound() {
        when(roleRepository.findByIdAndActiveTrue(1L))
                .thenReturn(Optional.of(role));

        assertThat(roleService.getByid(1L)).contains(role);
    }

    @Test
    void getByidFalse_shouldReturnInactiveRoleWhenFound() {
        when(roleRepository.findByIdAndActiveFalse(1L))
                .thenReturn(Optional.of(role));

        assertThat(roleService.getByidFalse(1L)).contains(role);
    }

    @Test
    void detailById_shouldThrowWhenRoleDoesNotExist() {
        when(roleRepository.findByIdAndActiveTrue(99L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> roleService.detailById(99L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Role not found with 99");
    }

    // ---------------------------------------------------------
    // SOFT DELETE ROLE
    // ---------------------------------------------------------

    @Test
    void deleteRole_shouldDeactivateRoleAndPublishInvalidationEvent() {
        List<Long> userIds = List.of(10L, 20L);

        when(roleRepository.findByIdAndActiveTrue(1L))
                .thenReturn(Optional.of(role));
        when(userRepository.findUserIdsByRoleId(1L))
                .thenReturn(userIds);

        roleService.deleteRole(1L);

        assertThat(role.isActive()).isFalse();

        verify(roleRepository).save(role);
        verify(eventPublisher).publishEvent(any(
                PermissionCacheInvalidationEvent.class));
    }

    @Test
    void deleteRole_shouldThrowWhenRoleDoesNotExist() {
        when(roleRepository.findByIdAndActiveTrue(99L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> roleService.deleteRole(99L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Role not found with 99");

        verify(roleRepository, never()).save(any(Role.class));
        verifyNoInteractions(userRepository, eventPublisher);
    }

    // ---------------------------------------------------------
    // RESTORE ROLE
    // ---------------------------------------------------------

    @Test
    void restoreRole_shouldActivateRoleAndInvalidateUserPermissionCache() {
        List<Long> userIds = List.of(10L, 20L);

        when(roleRepository.findByIdAndActiveFalse(1L))
                .thenReturn(Optional.of(role));
        when(userRepository.findUserIdsByRoleId(1L))
                .thenReturn(userIds);
        when(roleRepository.save(role)).thenReturn(role);
        when(roleMapper.toResponse(role)).thenReturn(response);

        RoleResponse result = roleService.restoreRole(1L);

        assertThat(result).isSameAs(response);
        assertThat(role.isActive()).isTrue();

        verify(roleRepository).save(role);
        verify(eventPublisher).publishEvent(any(
                PermissionCacheInvalidationEvent.class));
    }

    @Test
    void restoreRole_shouldThrowWhenRoleDoesNotExistOrIsNotInactive() {
        when(roleRepository.findByIdAndActiveFalse(99L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> roleService.restoreRole(99L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Role not found with 99");

        verify(roleRepository, never()).save(any(Role.class));
    }

    //    Test 1: Xác minh createRole() map request và gắn đúng role vào permission
    @Test
    void createRole_shouldMapRequestAndAssociatePermissionsWithSavedRole() {
        Permission permission = new Permission();

        when(request.getCode()).thenReturn("ADMIN");
        when(request.getPermissionIds()).thenReturn(List.of(1L));
        when(roleRepository.existsByCodeAndActiveTrue("ADMIN"))
                .thenReturn(false);
        when(roleRepository.save(any(Role.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(permissionRepository.findByIdIn(List.of(1L)))
                .thenReturn(List.of(permission));
        when(roleMapper.toResponse(any(Role.class)))
                .thenReturn(response);

        roleService.createRole(request);

        ArgumentCaptor<Role> roleCaptor = ArgumentCaptor.forClass(Role.class);

        verify(roleRepository).save(roleCaptor.capture());
        Role savedRole = roleCaptor.getValue();

        verify(modelMapper).map(request, savedRole);

        ArgumentCaptor<Iterable<RolePermission>> captor =
                ArgumentCaptor.forClass(Iterable.class);

        verify(rolePermissionRepository).saveAll(captor.capture());

        List<RolePermission> links =
                StreamSupport.stream(captor.getValue().spliterator(), false)
                        .toList();

        assertThat(links).hasSize(1);
        assertThat(links.get(0).getRole()).isSameAs(savedRole);
        assertThat(links.get(0).getPermission()).isSameAs(permission);
        assertThat(savedRole.getRolePermissions()).containsExactlyElementsOf(links);
    }

    //    Test 2: Xác minh xóa role thực sự chuyển từ active sang inactive
    @Test
    void deleteRole_shouldChangeActiveRoleToInactive() {
        role.setActive(true);

        when(roleRepository.findByIdAndActiveTrue(1L))
                .thenReturn(Optional.of(role));
        when(userRepository.findUserIdsByRoleId(1L))
                .thenReturn(List.of(10L, 20L));

        roleService.deleteRole(1L);

        assertThat(role.isActive()).isFalse();
        verify(roleRepository).save(role);
        verify(eventPublisher).publishEvent(any(
                PermissionCacheInvalidationEvent.class));
    }


    //    Test 3: Xác minh khôi phục role thực sự thay đổi trạng thái
    @Test
    void restoreRole_shouldChangeInactiveRoleToActive() {
        role.setActive(false);

        when(roleRepository.findByIdAndActiveFalse(1L))
                .thenReturn(Optional.of(role));
        when(userRepository.findUserIdsByRoleId(1L))
                .thenReturn(List.of(10L));
        when(roleRepository.save(role)).thenReturn(role);
        when(roleMapper.toResponse(role)).thenReturn(response);

        RoleResponse result = roleService.restoreRole(1L);

        assertThat(result).isSameAs(response);
        assertThat(role.isActive()).isTrue();

        verify(roleRepository).save(role);
        verify(eventPublisher).publishEvent(any(
                PermissionCacheInvalidationEvent.class));
    }

//    Test 4: Xác minh event chứa đúng user ID

    @Test
    void editRole_shouldPublishInvalidationEventForAffectedUsers() {
        List<Long> userIds = List.of(10L, 20L, 30L);

        when(roleRepository.findByIdAndActiveTrue(1L))
                .thenReturn(Optional.of(role));
        when(userRepository.findUserIdsByRoleId(1L))
                .thenReturn(userIds);
        when(request.getPermissionIds()).thenReturn(List.of());
        when(permissionRepository.findByIdIn(List.of()))
                .thenReturn(List.of());
        when(roleRepository.save(role)).thenReturn(role);
        when(roleMapper.toResponse(role)).thenReturn(response);

        roleService.editRole(1L, request);

        ArgumentCaptor<PermissionCacheInvalidationEvent> eventCaptor =
                ArgumentCaptor.forClass(
                        PermissionCacheInvalidationEvent.class);

        verify(eventPublisher).publishEvent(eventCaptor.capture());

        // Thay getUserIds() bằng getter thực tế của event nếu khác tên.
        assertThat(eventCaptor.getValue().userIds())
                .containsExactlyElementsOf(userIds);
    }

    //    Test 5: Xác minh metadata phân trang
    @Test
    void getPaginated_shouldReturnCorrectPaginationMetadata() {
        Page<Role> page = new PageImpl<>(
                List.of(role),
                PageRequest.of(1, 2, Sort.by("id").ascending()),
                5
        );

        when(roleRepository.findAll(any(Pageable.class)))
                .thenReturn(page);
        when(roleMapper.toResponse(role)).thenReturn(response);

        ResponseDTO<List<RoleResponse>> result =
                roleService.getPaginated(2, 2);

        assertThat(result.getResult()).containsExactly(response);
        assertThat(result.getMeta().getCurrent()).isEqualTo(2);
        assertThat(result.getMeta().getPageSize()).isEqualTo(2);
        assertThat(result.getMeta().getTotalPages()).isEqualTo(3);
        assertThat(result.getMeta().getTotalItems()).isEqualTo(5);
    }


}
