package com.webjob.application.Repository;

import com.webjob.application.Models.Resume;
import com.webjob.application.Models.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ResumeRepository extends JpaRepository<Resume,Long> {
    Page<Resume> findAllByUser(User user, Pageable pageable);

//    SELECT *
//    FROM resumes r
//    JOIN jobs j ON r.job_id = j.id
//    JOIN companies c ON j.company_id = c.id
//    WHERE c.id = :companyId
//    LIMIT ... OFFSET ...

    Page<Resume> findAllByJob_Company_Id(Long companyId, Pageable pageable);








}
