package com.webjob.application.repository;

import com.webjob.application.models.Entity.SavedJob;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SavedJobRepository extends JpaRepository<SavedJob,Long> {
    boolean existsByUserIdAndJobId(Long userId, Long jobId);


    @EntityGraph(attributePaths = "job")
    Optional<SavedJob> findByUserIdAndJobId(Long userId, Long jobId);

    @EntityGraph(attributePaths = "job")
    Page<SavedJob> findByUserId(Long userId, Pageable pageable);


}
