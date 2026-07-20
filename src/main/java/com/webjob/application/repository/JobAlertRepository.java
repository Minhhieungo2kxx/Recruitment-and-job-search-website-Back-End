package com.webjob.application.repository;

import com.webjob.application.models.Entity.JobAlert;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface JobAlertRepository extends JpaRepository<JobAlert,Long> {
    boolean existsByJobCategoryId(Long jobCategoryId);
}
