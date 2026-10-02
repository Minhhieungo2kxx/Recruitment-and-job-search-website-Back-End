package com.webjob.application.repository;

import com.webjob.application.enums.MatchStatus;
import com.webjob.application.enums.ResumeStatus;
import com.webjob.application.models.Entity.MatchResult;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MatchResultRepository extends JpaRepository<MatchResult, Long> {

    Optional<MatchResult> findByApplicationId(Long applicationId);


    @EntityGraph(attributePaths = {
            "skillDetails",
            "skillDetails.jobSkill",
            "skillDetails.jobSkill.skill",
            "skillDetails.candidateSkill",
            "application",
            "application.user"
    })
    Optional<MatchResult> findWithSkillDetailsByApplicationId(Long applicationId);

    boolean existsByApplicationId(Long applicationId);


    @Query(
            value = """
        SELECT mr
        FROM MatchResult mr
        JOIN FETCH mr.application a
        JOIN FETCH a.user
        WHERE a.job.id = :jobId
          AND (:minScore IS NULL OR mr.matchScore >= :minScore)
          AND (:maxScore IS NULL OR mr.matchScore <= :maxScore)
          AND (:status IS NULL OR mr.status = :status)
          AND (:resumeStatus IS NULL OR a.status = :resumeStatus)
        """,
            countQuery = """
        SELECT COUNT(mr)
        FROM MatchResult mr
        JOIN mr.application a
        WHERE a.job.id = :jobId
          AND (:minScore IS NULL OR mr.matchScore >= :minScore)
          AND (:maxScore IS NULL OR mr.matchScore <= :maxScore)
          AND (:status IS NULL OR mr.status = :status)
          AND (:resumeStatus IS NULL OR a.status = :resumeStatus)
        """
    )
    Page<MatchResult> findByJobIdWithFilter(
            @Param("jobId") Long jobId,
            @Param("minScore") Double minScore,
            @Param("maxScore") Double maxScore,
            @Param("status") MatchStatus status,
            @Param("resumeStatus") ResumeStatus resumeStatus,
            Pageable pageable
    );







}
