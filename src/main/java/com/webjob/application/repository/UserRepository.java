package com.webjob.application.repository;

import com.webjob.application.models.Entity.Company;
import com.webjob.application.models.Entity.Role;
import com.webjob.application.models.Entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long>, JpaSpecificationExecutor<User> {


    boolean existsByEmailAndDeletedFalse(String email);


    @SuppressWarnings("NullableProblems")
    boolean existsByIdAndDeletedFalse(Long id);

    User findByEmailAndDeletedFalse(String email);

    User findByEmail(String email);

    User findByRefreshTokenAndDeletedFalse(String refreshToken);


    User findByCompanyAndDeletedFalse(Company company);

    List<User> findAllByCompanyAndDeletedFalse(Company company);



    @Query("""
        SELECT u
        FROM User u
        WHERE u.deleted = false
          AND u.role.code LIKE 'HR_%'
          AND u.fullName LIKE %:searchTerm%
        """)
    List<User> findHRsByName(@Param("searchTerm") String searchTerm);

    @Query("""
        SELECT u
        FROM User u
        WHERE u.deleted = false
          AND u.role.code = 'USER'
          AND u.fullName LIKE %:searchTerm%
        """)
    List<User> findCandidatesByName(@Param("searchTerm") String searchTerm);


    @Query("""
        SELECT u
        FROM User u
        WHERE u.deleted = false
          AND u.isOnline = false
          AND u.lastSeenAt > :since
        """)
    List<User> findRecentlyOfflineUsers(@Param("since") Instant since);


    boolean existsByRoleAndDeletedFalse(Role role);

    @Query("""
        SELECT u
        FROM User u
        JOIN FETCH u.role r
        WHERE u.id = :id
          AND u.deleted = false
          AND r.active = true
        """)
    Optional<User> findActiveRoleUser(@Param("id") Long id);

    Optional<User> findByIdAndDeletedTrue(Long id);

    Optional<User> findByIdAndDeletedFalse(Long id);

    Page<User> findAllByDeletedFalse(Pageable pageable);


    Page<User> findAllByDeletedTrue(Pageable pageable);  // User đã xóa




}
