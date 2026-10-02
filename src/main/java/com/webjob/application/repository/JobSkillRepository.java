package com.webjob.application.repository;

import com.webjob.application.models.Entity.JobSkill;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface JobSkillRepository extends JpaRepository<JobSkill, Long> {
    Long countBySkillId(Long skillId);

    @Modifying
    @Query("delete from JobSkill js where js.skill.id = :skillId")
    void deleteBySkillId(Long skillId);


//    @Query("""
//                SELECT js
//                FROM JobSkill js
//                JOIN FETCH js.skill
//                WHERE js.job.id = :jobId
//            """)
//    List<JobSkill> findByJobId(@Param("jobId") Long jobId);

    @Query("""
        SELECT DISTINCT js
        FROM JobSkill js
        JOIN FETCH js.skill s
        LEFT JOIN FETCH s.aliases a
        WHERE js.job.id = :jobId
    """)
    List<JobSkill> findByJobId(@Param("jobId") Long jobId);



}
