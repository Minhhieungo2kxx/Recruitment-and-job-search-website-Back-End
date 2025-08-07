package com.webjob.application.Repository;

import com.webjob.application.Models.Entity.Job;
import com.webjob.application.Models.Entity.Skill;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface JobRepository extends JpaRepository<Job,Long>,JpaSpecificationExecutor<Job> {
    boolean existsByName(String name);

    List<Job> findAllBySkillsIn(List<Skill> skills);



}
