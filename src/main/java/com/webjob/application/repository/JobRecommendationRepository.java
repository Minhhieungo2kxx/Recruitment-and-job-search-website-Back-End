package com.webjob.application.repository;

import com.webjob.application.enums.RecommendationStatus;
import com.webjob.application.models.Entity.JobRecommendation;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface JobRecommendationRepository extends JpaRepository<JobRecommendation, Long> {


    @EntityGraph(attributePaths = {
            "items",
            "items.job",
            "items.job.company"
    })
    Optional<JobRecommendation> findFirstByUserIdAndStatusAndExpiresAtAfterOrderByCreatedAtDesc(
            Long userId,
            RecommendationStatus status,
            Instant now
    );


    // Đánh dấu hết hạn ngay thay vì xoá cứng — để debug/audit
    @Transactional
    @Modifying
    @Query("""
            UPDATE JobRecommendation r
            SET r.expiresAt = :now
            WHERE r.user.id = :userId
              AND r.status = 'COMPLETED'
              AND r.expiresAt > :now
            """)
    int invalidateByUserId(@Param("userId") Long userId, @Param("now") Instant now);

    @Transactional
    @Modifying
    @Query("""
            UPDATE JobRecommendation r
            SET r.expiresAt = :now
            WHERE r.user.id = :userId
              AND r.status = 'COMPLETED'
              AND r.expiresAt > :now
              AND r.createdAt < :minAgeThreshold
            """)
    int invalidateByUserIdIfOlderThan(
            @Param("userId") Long userId,
            @Param("now") Instant now,
            @Param("minAgeThreshold") Instant minAgeThreshold);


    List<JobRecommendation> findByResumeId(Long resumeId);


}
