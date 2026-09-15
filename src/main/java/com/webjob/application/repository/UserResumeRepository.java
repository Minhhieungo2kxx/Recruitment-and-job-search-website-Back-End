package com.webjob.application.repository;

import com.webjob.application.dto.Response.AdminResumeResponse;
import com.webjob.application.models.Entity.UserResume;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserResumeRepository extends JpaRepository<UserResume, Long> {
    Optional<UserResume> findByIdAndUserId(Long id, Long userId);

    List<UserResume> findAllByUserIdOrderByIsDefaultDescCreatedAtDesc(Long userId);

    Page<UserResume> findByUserId(Long userId, Pageable pageable);

    Optional<UserResume> findByUserIdAndIsDefaultTrue(Long userID);


    boolean existsByIdAndUserId(Long id, Long userId);


    @Query("""
            SELECT new com.webjob.application.dto.Response.AdminResumeResponse(
                    r.id,
                    r.name,
                    u.fullName,
                    u.email,
                    r.isDefault,
                    r.createdAt,
                    r.url,
                    COUNT(a.id)
            )
            FROM UserResume r
            JOIN r.user u
            LEFT JOIN r.applications a
            GROUP BY
                    r.id,
                    r.name,
                    u.fullName,
                    u.email,
                    r.isDefault,
                    r.createdAt,
                    r.url
            """)
    Page<AdminResumeResponse> getAllResumeForAdmin(Pageable pageable);


    @Modifying
    @Query("""
                UPDATE UserResume ur
                SET ur.isDefault = false
                WHERE ur.user.id = :userId
                  AND ur.isDefault = true
            """)
    void clearDefaultResume(Long userId);

    Long countByUserId(Long userId);

    Optional<UserResume> findFirstByUserIdOrderByCreatedAtDescIdDesc(Long userId);

}
