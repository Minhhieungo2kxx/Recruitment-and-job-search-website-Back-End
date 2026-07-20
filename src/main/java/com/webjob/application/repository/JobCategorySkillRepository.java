package com.webjob.application.repository;

import com.webjob.application.models.Entity.JobCategorySkill;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface JobCategorySkillRepository extends JpaRepository<JobCategorySkill,Long> {
    Long countBySkillId(Long skillId);
    @Modifying
    @Query("delete from JobCategorySkill jcs where jcs.skill.id = :skillId")
    void deleteBySkillId(Long skillId);
}
