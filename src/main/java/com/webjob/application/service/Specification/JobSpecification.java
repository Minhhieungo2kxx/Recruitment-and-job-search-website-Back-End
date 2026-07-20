package com.webjob.application.service.Specification;

import com.webjob.application.enums.*;
import com.webjob.application.models.Entity.Company;
import com.webjob.application.models.Entity.Job;
import com.webjob.application.models.Entity.JobSkill;
import com.webjob.application.models.Entity.Skill;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

public class JobSpecification {

//    client search
    public static Specification<Job> hasStatus(JobStatus status) {

        return (root, query, cb) -> {

            if (status == null) {
                return cb.conjunction();
            }

            return cb.equal(root.get("status"), status);
        };
    }
    public static Specification<Job> hasDeleted(Boolean deleted) {

        return (root, query, cb) -> {

            if (deleted == null) {
                return cb.conjunction();
            }

            return cb.equal(root.get("deleted"), deleted);
        };
    }
    public static Specification<Job> hasCompany(Long companyId) {

        return (root, query, cb) -> {

            if (companyId == null) {
                return cb.conjunction();
            }

            return cb.equal(
                    root.get("company").get("id"),
                    companyId
            );
        };
    }
    public static Specification<Job> createdBetween(Instant from, Instant to) {

        return (root, query, cb) -> {

            if (from == null && to == null) {
                return cb.conjunction();
            }

            if (from != null && to == null) {
                return cb.greaterThanOrEqualTo(root.get("createdAt"), from);
            }

            if (from == null) {
                return cb.lessThanOrEqualTo(root.get("createdAt"), to);
            }

            return cb.between(root.get("createdAt"), from, to);
        };
    }

    public static Specification<Job> notDeleted() {
        return (root, query, cb) ->
                cb.isFalse(root.get("deleted"));
    }

    public static Specification<Job> activeOnly() {

        return (root, query, cb) -> {

            Instant now = Instant.now();

            return cb.and(
                    cb.equal(root.get("status"), JobStatus.OPEN),
                    cb.lessThanOrEqualTo(root.get("startDate"), now),
                    cb.greaterThanOrEqualTo(root.get("endDate"), now)
            );
        };
    }

    public static Specification<Job> keyword(String keyword) {

        if (keyword == null || keyword.trim().isEmpty()) {
            return null;
        }

        String key = keyword.trim().toLowerCase();

        return (root, query, cb) -> {

            query.distinct(true);

            String value = "%" + key + "%";

            Join<Job, Company> company = root.join("company", JoinType.LEFT);
            Join<Job, JobSkill> skills = root.join("jobSkills", JoinType.LEFT);
            Join<JobSkill, Skill> skill = skills.join("skill", JoinType.LEFT);

            return cb.or(
                    cb.like(cb.lower(root.get("name")), value),
                    cb.like(cb.lower(cb.coalesce(root.get("description"), "")), value),
                    cb.like(cb.lower(cb.coalesce(root.get("requirement"), "")), value),
                    cb.like(cb.lower(cb.coalesce(root.get("responsibility"), "")), value),
                    cb.like(cb.lower(cb.coalesce(root.get("benefits"), "")), value),
                    cb.like(cb.lower(cb.coalesce(company.get("name"), "")), value),
                    cb.like(cb.lower(cb.coalesce(skill.get("name"), "")), value)
            );
        };
    }

    public static Specification<Job> hasLocation(String location) {
        return (root, query, cb) -> {

            if (location == null || location.trim().isEmpty()) {
                return cb.conjunction(); // không filter
            }

            return cb.like(cb.lower(root.get("location")), "%" + location.trim().toLowerCase() + "%"
            );
        };
    }

    public static Specification<Job> hasJobCategory(Long jobCategoryId) {

        return (root, query, cb) -> {

            if (jobCategoryId == null) {
                return cb.conjunction();
            }

            return cb.equal(root.get("jobCategory").get("id"), jobCategoryId
            );
        };
    }

    public static Specification<Job> hasSkills(List<Long> skillIds) {

        return (root, query, cb) -> {

            if (skillIds == null || skillIds.isEmpty()) {
                return cb.conjunction();
            }
            query.distinct(true);

            Join<Job, JobSkill> jobSkillJoin = root.join("jobSkills");
            Join<JobSkill, Skill> skillJoin = jobSkillJoin.join("skill");

            return skillJoin.get("id").in(skillIds);
        };
    }

    public static Specification<Job> hasSalary(Double minSalary, Double maxSalary) {

        return (root, query, cb) -> {

            if (minSalary == null && maxSalary == null) {
                return cb.conjunction();
            }

            if (minSalary != null && maxSalary == null) {
                return cb.greaterThanOrEqualTo(root.get("salaryMax"), minSalary);
            }

            if (minSalary == null) {
                return cb.lessThanOrEqualTo(root.get("salaryMin"), maxSalary);
            }

            // khoảng lương giao nhau
            return cb.and(
                    cb.lessThanOrEqualTo(root.get("salaryMin"), maxSalary),
                    cb.greaterThanOrEqualTo(root.get("salaryMax"), minSalary)
            );
        };

//        User:
//        10------20
//        8------12
//        overlap
//
//        15---------25
//        overlap
//
//        20------30
//        touch tại 20
//
//        5------9
//        không overlap
    }

    public static Specification<Job> hasExperience(Integer experience) {

        return (root, query, cb) -> {

            if (experience == null) {
                return cb.conjunction();
            }

            return cb.lessThanOrEqualTo(
                    root.get("experienceRequired"),
                    experience
            );
        };
    }

    public static Specification<Job> hasLevels(List<JobLevel> levels) {

        return (root, query, cb) -> {

            if (levels == null || levels.isEmpty()) {
                return cb.conjunction();
            }

            return root.get("level").in(levels);
        };
    }

    public static Specification<Job> hasWorkingTypes(List<WorkingType> workingTypes) {

        return (root, query, cb) -> {

            if (workingTypes == null || workingTypes.isEmpty()) {
                return cb.conjunction();
            }

            return root
                    .get("workingType")
                    .in(workingTypes);
        };
    }

    public static Specification<Job> hasWorkModes(
            List<WorkMode> workModes
    ) {

        return (root, query, cb) -> {

            if (workModes == null || workModes.isEmpty()) {
                return cb.conjunction();
            }

            return root
                    .get("workMode")
                    .in(workModes);
        };
    }

    public static Specification<Job> hasCompanies(List<Long> companyIds) {

        return (root, query, cb) -> {

            if (companyIds == null || companyIds.isEmpty()) {
                return cb.conjunction();
            }

            return root.get("company").get("id")
                    .in(companyIds);
        };
    }

    public static Specification<Job> createdWithin(
            PostedDateFilter filter
    ) {

        return (root, query, cb) -> {

            if (filter == null) {
                return cb.conjunction();
            }

            Instant now = Instant.now();

            Instant from = switch (filter) {

                case LAST_24_HOURS -> now.minus(24, ChronoUnit.HOURS);

                case LAST_3_DAYS -> now.minus(3, ChronoUnit.DAYS);

                case LAST_7_DAYS -> now.minus(7, ChronoUnit.DAYS);

                case LAST_30_DAYS -> now.minus(30, ChronoUnit.DAYS);
            };


            return cb.greaterThanOrEqualTo(
                    root.get("createdAt"),
                    from
            );
        };
    }
    public static Specification<Job> hasNegotiable(Boolean negotiable) {
        return (root, query, cb) ->
                negotiable == null
                        ? cb.conjunction()
                        : cb.equal(root.get("negotiable"), negotiable);
    }
    public static Specification<Job> companyActive() {
        return (root, query, cb) -> cb.and(
                cb.isFalse(root.get("company").get("deleted")),
                cb.equal(root.get("company").get("status"), CompanyStatus.ACTIVE)
        );
    }


}
