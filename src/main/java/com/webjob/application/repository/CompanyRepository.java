package com.webjob.application.repository;


import com.webjob.application.enums.CompanyStatus;
import com.webjob.application.models.Entity.Company;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface CompanyRepository extends JpaRepository<Company, Long>, JpaSpecificationExecutor<Company> {


    boolean existsByIdAndDeletedFalse(Long id);


    boolean existsByNameAndDeletedFalse(String name);

    boolean existsByName(String name);

    Optional<Company> findByIdAndDeletedFalse(Long id);

    Optional<Company> findByIdAndDeletedTrue(Long id);

    @Query("SELECT c FROM Company c LEFT JOIN FETCH c.industry WHERE c.id = :id")
    Optional<Company> findDetailById(@Param("id") Long id);

    @Override
    @EntityGraph(attributePaths = {"industry"})
    List<Company> findAll(Specification<Company> spec);

    @EntityGraph(attributePaths = "industry")
    List<Company> findByIdIn(Collection<Long> ids);
//
//    @Query("""
//                SELECT c.id
//                FROM Company c
//            """)
//    Page<Long> findAllIds(Pageable pageable);

    @EntityGraph(attributePaths = {"industry"})
    @Override
    Page<Company> findAll(Pageable pageable);


}
