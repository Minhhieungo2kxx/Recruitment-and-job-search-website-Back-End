package com.webjob.application.repository;

import com.webjob.application.enums.SkillStatus;
import com.webjob.application.models.Entity.SavedJob;
import com.webjob.application.models.Entity.SkillAlias;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface SkillAliasRepository extends JpaRepository<SkillAlias, Long> {

    @Query("""
                SELECT sa
                FROM SkillAlias sa
                JOIN FETCH sa.skill
                WHERE sa.skill.id IN :skillIds
            """)
    List<SkillAlias> findAllBySkillIds(@Param("skillIds") List<Long> skillIds);


    @Query("""
        SELECT sa
        FROM SkillAlias sa
        JOIN FETCH sa.skill s
        WHERE sa.status = :status
          AND s.status = :skillStatus
    """)
    List<SkillAlias> findAllActiveAliases(
            @Param("status") SkillStatus status,
            @Param("skillStatus") SkillStatus skillStatus
    );

    @Modifying
    @Query("DELETE FROM SkillAlias sa WHERE sa.skill.id = :skillId")
    void deleteBySkillId(@Param("skillId") Long skillId);
}
