package com.webjob.application.repository;

import com.webjob.application.models.Entity.Application;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ApplicationRepository extends JpaRepository<Application,Long>, JpaSpecificationExecutor<Application> {
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

    Optional<Application> findById(Long id);

    @Query("""
    select a
    from Application a
    join fetch a.job j
    join fetch j.company
    join fetch a.user
    join fetch a.resume
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



}
