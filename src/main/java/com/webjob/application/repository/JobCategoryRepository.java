package com.webjob.application.repository;

import com.webjob.application.models.Entity.JobCategory;
import com.webjob.application.models.Entity.Skill;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface JobCategoryRepository extends JpaRepository<JobCategory, Long>, JpaSpecificationExecutor<JobCategory> {

    boolean existsByName(String name);

    boolean existsByNameAndIdNot(String name, Long id);

    boolean existsByParentId(Long parentId);

    @Query("""
    SELECT DISTINCT jc
    FROM JobCategory jc
    LEFT JOIN FETCH jc.jobCategorySkills
    WHERE jc.id = :categoryId
""")
    Optional<JobCategory> findByIdWithSkills(@Param("categoryId") Long categoryId);

    @Query("""
            SELECT jc
            FROM JobCategory jc
            LEFT JOIN FETCH jc.parent
            ORDER BY jc.level ASC, jc.name ASC
            """)
    List<JobCategory> findAllTree();

    @EntityGraph(attributePaths = {
            "parent",
            "children"
    })
    @Query("select c from JobCategory c where c.id=:id")
    Optional<JobCategory> findDetailById(Long id);

}
