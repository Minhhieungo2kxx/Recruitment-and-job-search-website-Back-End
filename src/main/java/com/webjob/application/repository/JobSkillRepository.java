package com.webjob.application.repository;

import com.webjob.application.models.Entity.JobSkill;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface JobSkillRepository extends JpaRepository<JobSkill, Long> {
    Long countBySkillId(Long skillId);

    @Modifying
    @Query("delete from JobSkill js where js.skill.id = :skillId")
    void deleteBySkillId(Long skillId);



}
