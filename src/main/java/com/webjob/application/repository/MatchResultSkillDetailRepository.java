package com.webjob.application.repository;

import com.webjob.application.models.Entity.MatchResultSkillDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MatchResultSkillDetailRepository extends JpaRepository<MatchResultSkillDetail, Long> {

    List<MatchResultSkillDetail> findByMatchResultId(Long matchResultId);

    void deleteByMatchResultId(Long matchResultId);

}
