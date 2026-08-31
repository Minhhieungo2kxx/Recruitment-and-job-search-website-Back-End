package com.webjob.application.repository;

import com.webjob.application.dto.record.AlertMatchResult;
import com.webjob.application.dto.record.SkillMatchResult;
import com.webjob.application.enums.JobLevel;
import com.webjob.application.enums.WorkMode;
import com.webjob.application.enums.WorkingType;
import com.webjob.application.dto.Interface.JobCountDto;
import com.webjob.application.models.Entity.Job;
import com.webjob.application.models.Entity.Skill;
import jakarta.annotation.Nullable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;

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
            GROUP BY j
            ORDER BY
                COUNT(DISTINCT filterJs.skill) DESC,
                j.createdAt DESC
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

    @EntityGraph(attributePaths = "company")
    Optional<Job> findByIdAndDeletedFalse(Long id);

    Optional<Job> findByIdAndDeletedTrue(Long id);


    @Modifying
    @Query("""
    UPDATE Job j
    SET j.viewCount = COALESCE(j.viewCount, 0) + 1
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


    @Override
    @EntityGraph(attributePaths = {
            "company", "jobCategory",
    })
    List<Job> findAll(@Nullable Specification<Job> spec);


    @Query("SELECT j FROM Job j " +
            "LEFT JOIN FETCH j.company c " +
            "LEFT JOIN FETCH j.jobSkills js " +
            "LEFT JOIN FETCH js.skill s " +
            "WHERE j.id = :id")
    Optional<Job> findByIdWithDetails(@Param("id") Long id);



    @Query("""
    SELECT new com.webjob.application.dto.record.SkillMatchResult(
        j,
        COUNT(DISTINCT filterJs.skill)
    )
    FROM Job j
    JOIN j.jobSkills filterJs
            WHERE filterJs.skill.id IN :skills
                
    AND j.deleted = false
    AND j.status = com.webjob.application.enums.JobStatus.OPEN
    AND j.endDate >= :now
    GROUP BY j
    ORDER BY COUNT(DISTINCT filterJs.skill) DESC,
             j.createdAt DESC
    """)
    List<SkillMatchResult> findTop10BySkillsChatbox(
            @Param("skills") Set<Long> skillIds,
            @Param("now") Instant now,
            Pageable pageable
    );

    @Query("""
        SELECT new com.webjob.application.dto.record.AlertMatchResult(
            j,

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
            )
        )

        FROM Job j

        WHERE
            j.deleted = false
            AND j.status = com.webjob.application.enums.JobStatus.OPEN
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
    List<AlertMatchResult> findTopJobsForAlertChatbox(
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

    @EntityGraph(attributePaths = {
            "company",
            "jobCategory",
            "jobSkills",
            "jobSkills.skill"
    })
    List<Job> findByIdIn(List<Long> ids);

    @Query("SELECT j.id FROM Job j")
    Page<Long> findAllIds(Pageable pageable);




}
