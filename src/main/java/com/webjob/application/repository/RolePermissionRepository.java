package com.webjob.application.repository;

import com.webjob.application.models.Entity.Permission;
import com.webjob.application.models.Entity.Role;
import com.webjob.application.models.Entity.RolePermission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RolePermissionRepository extends JpaRepository<RolePermission, Long> {
    @Modifying
    @Query("delete from RolePermission rp where rp.permission.id = :permissionId")
    void deleteByPermissionId(Long permissionId);

    @Modifying
    @Query("delete from RolePermission rp where rp.role.id = :roleId")
    void deleteByRoleId(@Param("roleId") Long roleId);
}
