package com.webjob.application.repository;

import com.webjob.application.models.Entity.Industry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
@Repository
public interface IndustryRepository extends JpaRepository<Industry,Long>, JpaSpecificationExecutor<Industry> {

    Optional<Industry> findByIdAndDeletedFalse(Long id);

    Optional<Industry> findByIdAndDeletedTrue(Long id);

    List<Industry> findAllByDeletedFalseOrderByNameAsc();

    List<Industry> findAllByDeletedTrue();

    boolean existsByNameIgnoreCaseAndDeletedFalse(String name);


    Optional<Industry> findByNameIgnoreCaseAndDeletedFalse(String name);
}
