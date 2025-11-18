package com.webjob.application.Repository;

import com.webjob.application.Model.Entity.Job;
import com.webjob.application.Model.Entity.Skill;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
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


}
