package com.webjob.application.repository;

import com.webjob.application.enums.JobStatus;
import com.webjob.application.utils.common.JobCountDto;
import com.webjob.application.models.Entity.Job;
import com.webjob.application.models.Entity.Skill;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface JobRepository extends JpaRepository<Job, Long>, JpaSpecificationExecutor<Job> {

    boolean existsByNameAndDeletedFalse(String name);

    @Query("""
            SELECT DISTINCT j
            FROM Job j
            JOIN j.jobSkills js
            WHERE js.skill IN :skills
              AND j.deleted = false
            """)
    List<Job> findAllBySkills(@Param("skills") List<Skill> skills);

    @Query("""
            SELECT DISTINCT j
            FROM Job j
            JOIN j.jobSkills filterJs
            JOIN FETCH j.company
            LEFT JOIN FETCH j.jobCategory
            LEFT JOIN FETCH j.jobSkills js
            LEFT JOIN FETCH js.skill
            WHERE filterJs.skill IN :skills
            AND j.deleted = false
            AND j.status =com.webjob.application.enums.JobStatus.OPEN
            AND j.endDate >= :now
            ORDER BY j.createdAt DESC
            """)
    List<Job> findTop10BySkills(
            @Param("skills") List<Skill> skills,
            @Param("now") Instant now,
            Pageable pageable);


    @Modifying
    @Query("""
            UPDATE Job j
            SET j.appliedCount = j.appliedCount + 1
            WHERE j.id = :jobId
              AND j.deleted = false
            """)
    void increaseAppliedCount(@Param("jobId") Long jobId);

    @Modifying
    @Query("""
            UPDATE Job j
            SET j.appliedCount =
                CASE
                    WHEN j.appliedCount > 0
                    THEN j.appliedCount - 1
                    ELSE 0
                END
            WHERE j.id = :jobId
              AND j.deleted = false
            """)
    void decreaseAppliedCount(@Param("jobId") Long jobId);


    boolean existsByJobCategoryIdAndDeletedFalse(Long id);

    @Query("""
            SELECT jc.id AS categoryId,
                   COUNT(j.id) AS jobCount
            FROM JobCategory jc
            LEFT JOIN Job j
                   ON j.jobCategory = jc
                  AND j.deleted = false
            GROUP BY jc.id
            """)
    List<JobCountDto> countJobsByCategory();

//    Page<Job> findByDeletedFalse(Pageable pageable);

    Optional<Job> findByIdAndDeletedFalse(Long id);

    Optional<Job> findByIdAndDeletedTrue(Long id);

    @Modifying
    @Query("""
                UPDATE Job j
                SET j.viewCount = j.viewCount + 1
                WHERE j.id = :id
                  AND j.deleted = false
            """)
    int increaseViewCount(@Param("id") Long id);

    int countByCompanyIdAndDeletedFalse(Long companyId);


}
