package com.webjob.application.service.Specification;

import com.webjob.application.models.Entity.Industry;
import jakarta.persistence.criteria.CriteriaBuilder;
import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;

public class IndustrySpecification {
    public static Specification<Industry> hasKeyword(String keyword) {
        return (root, query, cb) -> {
            if (keyword == null || keyword.isBlank()) {
                return null;
            }

            String value = "%" + keyword.trim().toLowerCase() + "%";
            return cb.like(cb.lower(root.get("name")), value);
        };
    }
    public static Specification<Industry> hasStatus(Boolean active) {
        return (root, query, cb) -> {
            if (active == null) {
                return null; // Trả về null nghĩa là bỏ qua điều kiện lọc này (luôn đúng)
            }
            return cb.equal(root.get("active"), active);
        };
    }
    public static Specification<Industry> isDeleted(Boolean deleted) {
        return (root, query, cb) -> {
            if (deleted == null) {
                return null;
            }
            return cb.equal(root.get("deleted"), deleted);
        };
    }
    public static Specification<Industry> createdFrom(Instant from) {
        return (root, query, cb) -> {
            if (from == null) {
                return null;
            }

            return cb.greaterThanOrEqualTo(root.get("createdAt"), from);
        };
    }

    public static Specification<Industry> createdTo(Instant to) {
        return (root, query, cb) -> {
            if (to == null) {
                return null;
            }

            return cb.lessThanOrEqualTo(root.get("createdAt"), to);
        };
    }


}
