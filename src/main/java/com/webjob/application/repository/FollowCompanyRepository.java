package com.webjob.application.repository;

import com.webjob.application.models.Entity.FollowCompany;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FollowCompanyRepository extends JpaRepository<FollowCompany,Long> {

    int countByCompanyId(Long companyId);

}
