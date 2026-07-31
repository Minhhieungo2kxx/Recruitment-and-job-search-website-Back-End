package com.webjob.application.repository;

import com.webjob.application.models.Entity.FollowCompany;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface FollowCompanyRepository extends JpaRepository<FollowCompany, Long> {

    int countByCompanyId(Long companyId);

    boolean existsByUserIdAndCompanyId(Long userId, Long companyId);

    Optional<FollowCompany> findByUserIdAndCompanyId(Long userId, Long companyId);

    @EntityGraph(attributePaths = "company")
    Page<FollowCompany> findByUserId(Long userId, Pageable pageable);

    Optional<FollowCompany> findByIdAndUserId(Long id, Long userId);

    @Query("""
                SELECT fc
                FROM FollowCompany fc
                JOIN FETCH fc.user
                WHERE fc.company.id = :companyId
                AND fc.notificationEnabled = true
            """)
    List<FollowCompany> findFollowersWithNotification(@Param("companyId") Long companyId);

}
