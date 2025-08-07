package com.webjob.application.Repository;

import com.webjob.application.Models.Entity.Permission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PermissionRepository extends JpaRepository<Permission,Long> {

    boolean existsByApiPathAndMethodAndModule(String apiPath,String method,String Module);
    List<Permission> findByIdIn(List<Long> ids);
}
