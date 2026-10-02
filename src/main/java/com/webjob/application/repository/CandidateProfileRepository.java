package com.webjob.application.repository;

import com.webjob.application.models.Entity.CandidateProfile;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CandidateProfileRepository extends JpaRepository<CandidateProfile, Long> {
    Optional<CandidateProfile> findByResume_Id(Long resumeId);

    /** Load kèm skills để Scoring Engine không bị N+1 khi chấm điểm hàng loạt */
    @EntityGraph(attributePaths = {
            "skills",
            "skills.skill"
    })
    Optional<CandidateProfile> findWithSkillsByResume_Id(Long resumeId);

    boolean existsByResume_Id(Long resumeId);






}
