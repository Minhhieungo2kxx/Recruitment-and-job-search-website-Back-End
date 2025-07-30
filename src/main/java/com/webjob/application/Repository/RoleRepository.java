package com.webjob.application.Repository;

import com.webjob.application.Models.Role;
import org.hibernate.loader.ast.internal.LoaderHelper;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RoleRepository extends JpaRepository<Role,Long> {
    boolean existsByName(String name);
}
