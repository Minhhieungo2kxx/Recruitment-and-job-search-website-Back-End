package com.webjob.application.repository;

import com.webjob.application.models.Entity.Role;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RoleRepository extends JpaRepository<Role, Long> {

    boolean existsByCodeAndActiveTrue(String code);

    List<Role> findByActiveTrue();
    Optional<Role> findByCodeAndActiveTrue(String code);

    Optional<Role> findByIdAndActiveTrue(Long id);

    Optional<Role> findByIdAndActiveFalse(Long id);

    Page<Role> findByActiveTrue(Pageable pageable);

}
