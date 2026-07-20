package com.webjob.application.service;


import com.webjob.application.dto.Request.RoleRequest;
import com.webjob.application.dto.Response.RoleResponse;
import com.webjob.application.mapper.RoleMapper;
import com.webjob.application.models.Entity.Permission;
import com.webjob.application.models.Entity.Role;
import com.webjob.application.dto.Response.MetaDTO;
import com.webjob.application.dto.Response.ResponseDTO;
import com.webjob.application.models.Entity.RolePermission;
import com.webjob.application.models.Entity.User;
import com.webjob.application.repository.PermissionRepository;
import com.webjob.application.repository.RolePermissionRepository;
import com.webjob.application.repository.RoleRepository;
import com.webjob.application.repository.UserRepository;
import com.webjob.application.service.Redis.PermissionCacheService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class RoleService {

    private final RoleRepository roleRepository;

    private final PermissionRepository permissionRepository;
    private final PermissionCacheService permissionCacheService;

    private final ModelMapper modelMapper;
    private final RolePermissionRepository rolePermissionRepository;

    private final RoleMapper roleMapper; // Tiêm mapper vào đây

    private final UserRepository userRepository;


    @Transactional
    public RoleResponse createRole(RoleRequest request) {
        if (roleRepository.existsByCodeAndActiveTrue(request.getCode())) {
            throw new IllegalArgumentException(
                    "Role đã tồn tại: " + request.getCode());
        }
        Role role = new Role();
        modelMapper.map(request, role);
        Role saved = roleRepository.save(role);
        List<Permission> permissions = getValidPerMissions(request.getPermissionIds());
        List<RolePermission> rolePermissions = new ArrayList<>();
        if (!permissions.isEmpty()) {
            for (Permission permission : permissions) {

                RolePermission rp = new RolePermission();
                rp.setRole(saved);

                rp.setPermission(permission);
                rolePermissions.add(rp);
            }
            rolePermissionRepository.saveAll(rolePermissions);
        }
        saved.setRolePermissions(rolePermissions);
        return roleMapper.toResponse(saved);
    }

    @Transactional
    public RoleResponse editRole(Long id, RoleRequest request) {
        Role role = roleRepository.findByIdAndActiveTrue(id)
                .orElseThrow(() -> new IllegalArgumentException("Role not found"));

        modelMapper.map(request, role);

        rolePermissionRepository.deleteByRoleId(role.getId());
        List<Permission> permissions = getValidPerMissions(request.getPermissionIds());

        List<RolePermission> rolePermissions = new ArrayList<>();
        if (!permissions.isEmpty()) {
            for (Permission permission : permissions) {

                RolePermission rp = new RolePermission();
                rp.setRole(role);

                rp.setPermission(permission);
                rolePermissions.add(rp);
            }
            rolePermissionRepository.saveAll(rolePermissions);
        }

        rolePermissionRepository.saveAll(rolePermissions);

        role.setRolePermissions(rolePermissions);

        return roleMapper.toResponse(roleRepository.save(role));
    }

    public Page<Role> getAllPage(int page, int size) {
        Sort.Direction direction = Sort.Direction.ASC;
        Sort sort = Sort.by(direction, "id");
        Pageable pageable = PageRequest.of(page, size, sort);
        return roleRepository.findAll(pageable);
    }


    private List<Permission> getValidPerMissions(List<Long> ids) {
        return permissionRepository.findByIdIn(ids);
    }

    public Optional<Role> getByid(Long id) {
        return roleRepository.findByIdAndActiveTrue(id);
    }
    public Role detailById(Long id){
        return getByid(id).orElseThrow(() ->
                new IllegalArgumentException("Role not found with " + id));
    }
    public Optional<Role> getByidFalse(Long id) {
        return roleRepository.findByIdAndActiveFalse(id);
    }

    @Transactional
    public void deleteRole(Long id) {
        Role role = getByid(id).orElseThrow(() -> new IllegalArgumentException("Role not found with " + id));
        if (userRepository.existsByRoleAndDeletedFalse(role)) {
            throw new IllegalArgumentException(
                    "Role đang được sử dụng bởi User.");
        }
        role.setActive(false);
        for (User user : role.getUsers()) {
            permissionCacheService.evict(user.getId().toString());
        }
        roleRepository.save(role);
    }



    public ResponseDTO<List<RoleResponse>> getPaginated(int page,int size) {

        try {

            if (page <= 0)
                page = 1;
            if(size<=0)
                size=10;
        } catch (NumberFormatException e) {
            // Nếu người dùng nhập sai, mặc định về trang đầu
            page = 1;
            size=10;
        }
        Page<Role> pagelist = getAllPage(page - 1, size);
        int currentpage = pagelist.getNumber() + 1;
        int pagesize = pagelist.getSize();
        int totalpage = pagelist.getTotalPages();
        Long totalItem = pagelist.getTotalElements();

        MetaDTO metaDTO = new MetaDTO(currentpage, pagesize, totalpage, totalItem);
        List<Role> jobsList = pagelist.getContent();
        List<RoleResponse> roleResponseList = jobsList.stream()
                .map(roleMapper::toResponse)
                .toList();
        ResponseDTO<List<RoleResponse>> respond = new ResponseDTO<>(metaDTO, roleResponseList);
        return respond;
    }
    @Transactional
    public RoleResponse restoreRole(Long id) {
        Role role = getByidFalse(id)
                .orElseThrow(() -> new IllegalArgumentException("Role not found with " + id));

        if (role.isActive()) {
            throw new IllegalArgumentException("Role đã ở trạng thái hoạt động.");
        }
        role.setActive(true);
        Role restore=roleRepository.save(role);
        for (User user : role.getUsers()) {
            permissionCacheService.evict(user.getId().toString());
        }
        return roleMapper.toResponse(restore);
    }


}
