package com.webjob.application.Repository;

import com.webjob.application.Models.Skill;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Repository
public interface SkillRepository extends JpaRepository<Skill,Long> {
    boolean existsByName(String name);
    boolean existsById(Long id);
    List<Skill> findByIdIn(List<Long> ids);


}
