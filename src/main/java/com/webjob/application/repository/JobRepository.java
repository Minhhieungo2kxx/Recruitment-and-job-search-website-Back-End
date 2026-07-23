package com.webjob.application.repository;

import com.webjob.application.enums.JobLevel;
import com.webjob.application.enums.JobStatus;
import com.webjob.application.enums.WorkMode;
import com.webjob.application.enums.WorkingType;
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
            AND (
                j.createdAt > :lastCheckedAt
                OR j.updatedAt > :lastCheckedAt
            )
            ORDER BY j.createdAt DESC
            """)
    List<Job> findTop10BySkills(
            @Param("skills") List<Skill> skills,
            @Param("now") Instant now,
            @Param("lastCheckedAt") Instant lastCheckedAt,
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

    @Query("""
                SELECT j
                FROM Job j
                JOIN FETCH j.company
                WHERE 
                    j.deleted = false
                    AND j.status = 'OPEN'
                    AND j.endDate > CURRENT_TIMESTAMP

                    AND (
                        :keyword IS NULL 
                        OR LOWER(j.name) LIKE LOWER(CONCAT('%', :keyword, '%'))
                        OR LOWER(j.description) LIKE LOWER(CONCAT('%', :keyword, '%'))
                    )

                ORDER BY

                (
                    CASE 
                        WHEN :keyword IS NOT NULL 
                        AND LOWER(j.name) LIKE LOWER(CONCAT('%', :keyword, '%'))
                        THEN 40
                        ELSE 0
                    END

                    +

                    CASE 
                        WHEN :keyword IS NOT NULL 
                        AND LOWER(j.description) LIKE LOWER(CONCAT('%', :keyword, '%'))
                        THEN 15
                        ELSE 0
                    END


                    +

                    CASE
                        WHEN :location IS NOT NULL
                        AND LOWER(j.location) LIKE LOWER(CONCAT('%', :location, '%'))
                        THEN 20
                        ELSE 0
                    END


                    +

                    CASE
                        WHEN :categoryId IS NOT NULL
                        AND j.jobCategory.id = :categoryId
                        THEN 30
                        ELSE 0
                    END


                    +

                    CASE
                        WHEN :level IS NOT NULL
                        AND j.level = :level
                        THEN 15
                        ELSE 0
                    END


                    +

                    CASE
                        WHEN :workMode IS NOT NULL
                        AND j.workMode = :workMode
                        THEN 15
                        ELSE 0
                    END
                    
                    +
                    
                    CASE
                        WHEN :workingType IS NOT NULL
                        AND j.workingType = :workingType
                        THEN 15
                        ELSE 0
                    END


                    +

                    CASE
                        WHEN :salaryMin IS NOT NULL
                        AND :salaryMax IS NOT NULL
                        AND j.salaryMin <= :salaryMax
                        AND j.salaryMax >= :salaryMin
                        THEN 25
                        ELSE 0
                    END

                ) DESC,

                j.createdAt DESC
            """)
    List<Job> findTopJobsForAlert(
            @Param("keyword") String keyword,
            @Param("location") String location,
            @Param("categoryId") Long categoryId,
            @Param("level") JobLevel level,
            @Param("workMode") WorkMode workMode,
            @Param("salaryMin") Double salaryMin,
            @Param("salaryMax") Double salaryMax,
            @Param("workingType") WorkingType workingType,
            Pageable pageable
    );


}
