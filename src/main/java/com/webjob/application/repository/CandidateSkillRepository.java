package com.webjob.application.repository;

import com.webjob.application.models.Entity.CandidateSkill;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CandidateSkillRepository extends JpaRepository<CandidateSkill, Long> {

    List<CandidateSkill> findByCandidateProfileId(Long candidateProfileId);

    @Modifying
    @Query("DELETE FROM CandidateSkill cs WHERE cs.candidateProfile.id = :profileId")
    void deleteByCandidateProfileId(@Param("profileId") Long candidateProfileId);
}
