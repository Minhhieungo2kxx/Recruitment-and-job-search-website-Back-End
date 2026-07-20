package com.webjob.application.service.Specification;

import com.webjob.application.enums.CategoryStatus;
import com.webjob.application.models.Entity.JobCategory;
import org.springframework.data.jpa.domain.Specification;

public class JobCategorySpecification {
    public static Specification<JobCategory> hasKeyword(String keyword) {
        return (root, query, cb) -> {
            if (keyword == null || keyword.isBlank()) {
                return cb.conjunction();
            }

            return cb.like(
                    cb.lower(root.get("name")),
                    "%" + keyword.toLowerCase() + "%"
            );
        };
    }

    public static Specification<JobCategory> hasStatus(CategoryStatus status) {
        return (root, query, cb) -> {
            if (status == null) {
                return cb.conjunction();
            }

            return cb.equal(root.get("status"), status);
        };
    }
}
