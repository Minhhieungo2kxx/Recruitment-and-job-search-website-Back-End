package com.webjob.application.repository;

import com.webjob.application.models.Entity.Job;
import com.webjob.application.models.Entity.Skill;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface JobRepository extends JpaRepository<Job, Long>, JpaSpecificationExecutor<Job> {
    boolean existsByName(String name);

    List<Job> findAllBySkillsIn(List<Skill> skills);

    List<Job> findDistinctBySkillsIn(List<Skill> skills);


    @Query("""
            SELECT DISTINCT j 
            FROM Job j 
            JOIN j.skills s 
            WHERE s IN :skills
            ORDER BY j.createdAt DESC
            """)
    List<Job> findTop10BySkills(@Param("skills") List<Skill> skills, Pageable pageable);

    @Modifying
    @Query("""
        UPDATE Job j
        SET j.appliedCount = j.appliedCount + 1
        WHERE j.id = :jobId
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
""")
    void decreaseAppliedCount(@Param("jobId") Long jobId);


}
