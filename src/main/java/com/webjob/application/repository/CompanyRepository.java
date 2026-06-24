package com.webjob.application.repository;


import com.webjob.application.models.Entity.Company;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CompanyRepository extends JpaRepository<Company,Long>, JpaSpecificationExecutor<Company> {


     boolean existsById(Long id);
     Optional<Company> findById(Long id);


}
