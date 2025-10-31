package com.webjob.application.Service.Specification;

import com.webjob.application.Model.Entity.Job;
import com.webjob.application.Model.Enums.CompetitionLevel;
import com.webjob.application.Model.Enums.JobCategory;
import jakarta.persistence.criteria.Join;
import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;
import java.util.List;

public class JobSpecification {
    public static Specification<Job> hasNameLike(String name) {
        return (root, query, cb) -> {
            if (name == null || name.trim().isEmpty()) {
                return cb.conjunction(); // always true
            }
            return cb.like(cb.lower(root.get("name")), "%" + name.toLowerCase() + "%");
        };
    }

    public static Specification<Job> hasLocationLike(String location) {
        return (root, query, cb) -> {
            if (location == null || location.trim().isEmpty()) {
                return cb.conjunction();
            }
            return cb.like(cb.lower(root.get("location")), "%" + location.toLowerCase() + "%");
        };
    }

    public static Specification<Job> hasLevel(String level) {
        return (root, query, cb) -> {
            if (level == null || level.trim().isEmpty()) {
                return cb.conjunction();
            }
            return cb.equal(root.get("level"), level);
        };
    }

    public static Specification<Job> hasDescriptionLike(String description) {
        return (root, query, cb) -> {
            if (description == null || description.trim().isEmpty()) {
                return cb.conjunction();
            }
            return cb.like(cb.lower(root.get("description")), "%" + description.toLowerCase() + "%");
        };
    }

    public static Specification<Job> hasSalaryBetween(Double minSalary, Double maxSalary) {
        return (root, query, cb) -> {
            // Không có điều kiện nào được truyền -> bỏ qua (luôn đúng)
            if (minSalary == null && maxSalary == null) {
                return cb.conjunction();
            }
            // Có cả min và max -> salary BETWEEN min AND max
            if (minSalary != null && maxSalary != null) {
                return cb.between(root.get("salary"), minSalary, maxSalary);
            }

            // Chỉ có min -> salary >= min
            if (minSalary != null) {
                return cb.greaterThanOrEqualTo(root.get("salary"), minSalary);
            }

            // Chỉ có max -> salary <= max
            return cb.lessThanOrEqualTo(root.get("salary"), maxSalary);
        };
    }

    public static Specification<Job> hasDateRange(Instant from, Instant to) {
        return (root, query, cb) -> {
            if (from == null && to == null) {
                return cb.conjunction(); // luôn đúng
            }

            if (from != null && to != null) {
                return cb.and(
                        cb.greaterThanOrEqualTo(root.get("startDate"), from),
                        cb.lessThanOrEqualTo(root.get("endDate"), to)
                );
            }

            if (from != null) {
                return cb.greaterThanOrEqualTo(root.get("startDate"), from);
            }

            return cb.lessThanOrEqualTo(root.get("endDate"), to);
        };
    }


    public static Specification<Job> isActive(Boolean active) {
        return (root, query, cb) -> {
            if (active == null) {
                return cb.conjunction();
            }
            return cb.equal(root.get("active"), active);
        };
    }

    public static Specification<Job> hasSkills(List<Long> skillIds) {
        return (root, query, cb) -> {
            if (skillIds == null || skillIds.isEmpty()) {
                return cb.conjunction();
            }
            Join<Object, Object> skillsJoin = root.join("skills");
            return skillsJoin.get("id").in(skillIds);
        };
    }

    public static Specification<Job> hasCompetitionLevel(CompetitionLevel competitionLevel) {
        return (root, query, cb) -> {
            if (competitionLevel == null) {
                return cb.conjunction();
            }
            return cb.equal(root.get("competitionLevel"), competitionLevel);
        };
    }

    public static Specification<Job> hasJobCategory(JobCategory jobCategory) {
        return (root, query, cb) -> {
            if (jobCategory == null) {
                return cb.conjunction();
            }
            return cb.equal(root.get("jobCategory"), jobCategory);
        };

    }
}
