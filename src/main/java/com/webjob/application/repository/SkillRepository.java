package com.webjob.application.repository;

import com.webjob.application.enums.SkillStatus;
import com.webjob.application.models.Entity.Skill;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SkillRepository extends JpaRepository<Skill, Long>, JpaSpecificationExecutor<Skill> {
    boolean existsByName(String name);

    boolean existsById(Long id);

    List<Skill> findByIdIn(List<Long> ids);

    boolean existsByNameIgnoreCase(String name);

    boolean existsByNameAndIdNot(String name,Long id);


    @Query("SELECT s.id FROM Skill s WHERE s.name IN :names")
    List<Long> findIdsByNameIn(@Param("names") List<String> names);

    @Query("SELECT s FROM Skill s WHERE s.status = :status")
    List<Skill> findAllByStatus(SkillStatus status);

    @EntityGraph(attributePaths = "aliases")
    Optional<Skill> findById(Long id);









}
