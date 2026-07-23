package com.webjob.application.repository;

import com.webjob.application.enums.JobLevel;
import com.webjob.application.enums.WorkMode;
import com.webjob.application.models.Entity.JobAlert;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface JobAlertRepository extends JpaRepository<JobAlert, Long>, JpaSpecificationExecutor<JobAlert> {
    boolean existsByJobCategoryId(Long jobCategoryId);

    List<JobAlert> findByUserId(Long userId);

    List<JobAlert> findByUserIdAndActiveTrue(Long userId);

    @EntityGraph(attributePaths = {"jobCategory"})
    Optional<JobAlert> findByIdAndUserId(Long id, Long userId);



    long countByUserId(Long userId);

    boolean existsByUserIdAndKeywordAndLocationAndJobCategoryIdAndLevelAndWorkMode(
            Long userId,
            String keyword,
            String location,
            Long categoryId,
            JobLevel level,
            WorkMode workMode
    );

    @EntityGraph(attributePaths = {"jobCategory"})
    Page<JobAlert> findAll(Pageable pageable);

    @Query("""
                select ja.id
                from JobAlert ja
                where ja.active = true
                  and ja.nextRunAt <= :now
                order by ja.nextRunAt asc, ja.id asc
            """)
    Page<Long> findIdsToProcess(@Param("now") Instant now, Pageable pageable);

    @EntityGraph(attributePaths = {"user", "jobCategory"})
    Optional<JobAlert> findById(Long id);
}
