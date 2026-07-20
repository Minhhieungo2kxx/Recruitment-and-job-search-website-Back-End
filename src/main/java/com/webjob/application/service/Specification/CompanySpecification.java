package com.webjob.application.service.Specification;

import com.webjob.application.enums.CompanyStatus;
import com.webjob.application.models.Entity.Company;

import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;
import java.util.List;

public class CompanySpecification {
    public static Specification<Company> hasKeyword(String keyword) {
        return (root, query, cb) -> {
            if (keyword == null || keyword.isBlank()) {
                return null;
            }

            String value = "%" + keyword.trim().toLowerCase() + "%";

            return cb.or(
                    cb.like(cb.lower(root.get("name")), value),
                    cb.like(cb.lower(root.get("description")), value)
            );
        };
    }
    public static Specification<Company> hasIndustry(String industry) {
        return (root, query, cb) -> {
            if (industry == null || industry.isBlank()) {
                return null;
            }

            return cb.equal(root.get("industry"), industry);
        };
    }

    public static Specification<Company> hasStatuses(List<CompanyStatus> statuses) {

        return (root, query, cb) -> {

            if (statuses == null || statuses.isEmpty()) {
                return null;
            }

            return root.get("status").in(statuses);
        };
    }

    public static Specification<Company> employeeSizeGreaterThan(Integer size) {
        return (root, query, cb) -> {
            if (size == null) {
                return null;
            }
            return cb.greaterThanOrEqualTo(root.get("employeeSize"), size);
        };
    }

    public static Specification<Company> employeeSizeLessThan(Integer size) {
        return (root, query, cb) -> {
            if (size == null) {
                return null;
            }
            return cb.lessThanOrEqualTo(root.get("employeeSize"), size);
        };
    }

    public static Specification<Company> foundedYearFrom(Integer year) {
        return (root, query, cb) -> {
            if (year == null) {
                return null;
            }
            return cb.greaterThanOrEqualTo(root.get("foundedYear"), year);
        };
    }

    public static Specification<Company> foundedYearTo(Integer year) {
        return (root, query, cb) -> {
            if (year == null) {
                return null;
            }
            return cb.lessThanOrEqualTo(root.get("foundedYear"), year);
        };
    }

    public static Specification<Company> hasTaxCode(String taxCode) {
        return (root, query, cb) -> {
            if (taxCode == null || taxCode.isBlank()) {
                return null;
            }
            return cb.equal(root.get("taxCode"), taxCode);
        };
    }

    public static Specification<Company> hasEmail(String email) {
        return (root, query, cb) -> {
            if (email == null || email.isBlank()) {
                return null;
            }
            return cb.like(cb.lower(root.get("email")),
                    "%" + email.toLowerCase() + "%");
        };
    }

    public static Specification<Company> hasPhone(String phone) {
        return (root, query, cb) -> {
            if (phone == null || phone.isBlank()) {
                return null;
            }
            return cb.like(root.get("phone"), "%" + phone + "%");
        };
    }

    public static Specification<Company> isDeleted(Boolean deleted) {
        return (root, query, cb) -> {
            if (deleted == null) {
                return null;
            }
            return cb.equal(root.get("deleted"), deleted);
        };
    }


    public static Specification<Company> createdFrom(Instant from) {
        return (root, query, cb) -> {
            if (from == null) {
                return null;
            }

            return cb.greaterThanOrEqualTo(root.get("createdAt"), from);
        };
    }

    public static Specification<Company> createdTo(Instant to) {
        return (root, query, cb) -> {
            if (to == null) {
                return null;
            }

            return cb.lessThanOrEqualTo(root.get("createdAt"), to);
        };
    }

    public static Specification<Company> hasJobs(Boolean hasJobs) {

        return (root, query, cb) -> {

            if (hasJobs == null) {
                return null;
            }

            if (hasJobs) {

                return cb.isNotEmpty(root.get("jobs"));
            }

            return cb.isEmpty(root.get("jobs"));
        };
    }

    public static Specification<Company> visible() {
        return (root, query, cb) ->
                cb.isFalse(root.get("deleted"));
    }

    public static Specification<Company> active() {
        return (root, query, cb) ->
                cb.equal(root.get("status"), CompanyStatus.ACTIVE);
    }

}
