package com.webjob.application.Repository;

import com.webjob.application.Model.Entity.Resume;
import com.webjob.application.Model.Entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface ResumeRepository extends JpaRepository<Resume,Long>, JpaSpecificationExecutor<Resume> {
    Page<Resume> findAllByUser(User user, Pageable pageable);

//    SELECT *
//    FROM resumes r
//    JOIN jobs j ON r.job_id = j.id
//    JOIN companies c ON j.company_id = c.id
//    WHERE c.id = :companyId
//    LIMIT ... OFFSET ...
    Page<Resume> findAllByJob_Company_Id(Long companyId, Pageable pageable);








}
