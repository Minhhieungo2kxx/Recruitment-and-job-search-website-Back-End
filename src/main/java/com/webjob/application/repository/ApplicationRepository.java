package com.webjob.application.repository;

import com.webjob.application.enums.ResumeStatus;
import com.webjob.application.models.Entity.Application;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ApplicationRepository extends JpaRepository<Application, Long>, JpaSpecificationExecutor<Application> {
    boolean existsByUserIdAndJobId(Long userId, Long jobId);

    boolean existsByResumeId(Long resumeId);


    @Query("""
                SELECT a
                FROM Application a
                JOIN FETCH a.user
                JOIN FETCH a.resume
                JOIN FETCH a.job
                WHERE a.job.company.id = :companyId
                ORDER BY a.createdAt DESC
            """)
    Page<Application> findApplicationsByCompany(Long companyId, Pageable pageable);


    @EntityGraph(attributePaths = {"job", "user"})
    Optional<Application> findById(Long id);

    @Query("""
                select a
                from Application a
                join fetch a.job j
                join fetch j.company
                join fetch a.user
                join fetch a.resume
                LEFT JOIN FETCH a.reviewedBy
                where a.id=:id
            """)
    Optional<Application> findDetailById(Long id);

    @Query("""
                SELECT a
                FROM Application a
                JOIN FETCH a.user
                JOIN FETCH a.job
                JOIN FETCH a.resume
                WHERE a.id = :id
                AND a.user.id = :userId
            """)
    Optional<Application> findDetailByIdAndUserId(
            Long id,
            Long userId
    );


    @Override
    @EntityGraph(attributePaths = {"user", "job", "resume"})
    Page<Application> findAll(Pageable pageable);


    @Query("""
            SELECT 
                COUNT(a),
                SUM(CASE WHEN a.status = 'PENDING' THEN 1 ELSE 0 END),
                SUM(CASE WHEN a.status = 'REVIEWING' THEN 1 ELSE 0 END),
                SUM(CASE WHEN a.status = 'INTERVIEWING' THEN 1 ELSE 0 END),
                SUM(CASE WHEN a.status = 'OFFERED' THEN 1 ELSE 0 END),
                SUM(CASE WHEN a.status = 'HIRED' THEN 1 ELSE 0 END),
                SUM(CASE WHEN a.status = 'REJECTED' THEN 1 ELSE 0 END),
                MAX(a.createdAt)
            FROM Application a
            WHERE a.user.id = :userId
            """)
    Object[] getApplicationSummary(@Param("userId") Long userId);


    @Query("""
            SELECT a
            FROM Application a
            JOIN FETCH a.job j
            LEFT JOIN FETCH j.company c
            WHERE a.user.id = :userId
            AND (:status IS NULL OR a.status = :status)
            AND (
                :keyword IS NULL
                OR LOWER(j.name) LIKE LOWER(CONCAT('%', :keyword, '%'))
                OR LOWER(c.name) LIKE LOWER(CONCAT('%', :keyword, '%'))
            )
            """)
    List<Application> searchApplications(
            Long userId,
            ResumeStatus status,
            String keyword,
            Pageable pageable
    );


}
