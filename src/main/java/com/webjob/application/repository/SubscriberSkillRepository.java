package com.webjob.application.repository;

import com.webjob.application.models.Entity.SubscriberSkill;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface SubscriberSkillRepository extends JpaRepository<SubscriberSkill, Long> {
    Long countBySkillId(Long skillId);
    @Modifying
    @Query("delete from SubscriberSkill ss where ss.skill.id = :skillId")
    void deleteBySkillId(Long skillId);

}
