package com.webjob.application.mapper;

import com.webjob.application.dto.Response.PermissionResponse;
import com.webjob.application.dto.Response.RoleResponse;
import com.webjob.application.models.Entity.Role;
import org.springframework.stereotype.Component;

@Component
public class RoleMapper {
    public RoleResponse toResponse(Role role) {
        if(role==null){
            return null;
        }

        return RoleResponse.builder()
                .id(role.getId())
                .code(role.getCode())
                .displayName(role.getDisplayName())
                .description(role.getDescription())
                .active(role.isActive())
                .createdAt(role.getCreatedAt())
                .createdBy(role.getCreatedBy())
                .updatedAt(role.getUpdatedAt())
                .updatedBy(role.getUpdatedBy())
                .permissions(
                        role.getRolePermissions()
                                .stream()
                                .map(rp ->
                                        PermissionResponse.builder()
                                                .id(rp.getPermission().getId())
                                                .code(rp.getPermission().getCode())
                                                .name(rp.getPermission().getName())
                                                .build()
                                )
                                .toList()
                )
                .build();
    }
}
