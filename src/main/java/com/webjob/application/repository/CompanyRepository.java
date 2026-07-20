package com.webjob.application.repository;


import com.webjob.application.enums.CompanyStatus;
import com.webjob.application.models.Entity.Company;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CompanyRepository extends JpaRepository<Company, Long>, JpaSpecificationExecutor<Company> {


    boolean existsByIdAndDeletedFalse(Long id);

    // Spring JPA tự động sinh câu truy vấn kiểm tra tồn tại dựa trên tên hàm
    boolean existsByNameAndDeletedFalse(String name);

    boolean existsByName(String name);

    Optional<Company> findByIdAndDeletedFalse(Long id);

    Optional<Company> findByIdAndDeletedTrue(Long id);




}
