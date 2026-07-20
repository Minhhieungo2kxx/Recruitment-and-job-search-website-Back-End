package com.webjob.application.service.Specification;

import com.webjob.application.enums.SubscriberStatus;
import com.webjob.application.models.Entity.Application;
import com.webjob.application.models.Entity.Skill;
import com.webjob.application.models.Entity.Subscriber;
import com.webjob.application.models.Entity.SubscriberSkill;
import jakarta.persistence.criteria.Join;
import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;
import java.util.List;

public class SubscriberSpecification {

    public static Specification<Subscriber> keyword(String keyword) {

        return (root, query, cb) -> {

            if (keyword == null || keyword.isBlank()) {
                return cb.conjunction();
            }

            String value = "%" + keyword.toLowerCase() + "%";

            return cb.or(

                    cb.like(cb.lower(root.get("name")), value),

                    cb.like(root.get("phoneNumber"), value)

            );

        };

    }
    public static Specification<Subscriber> hasUserId(Long userId) {
        return (root, query, cb) ->
                cb.equal(root.get("user").get("id"), userId);
    }


    public static Specification<Subscriber> hasStatus(
            SubscriberStatus status
    ) {

        return (root, query, cb) -> {

            if (status == null) {
                return cb.conjunction();
            }

            return cb.equal(root.get("status"), status);

        };
    }
    public static Specification<Subscriber> createdAfter(
            Instant from
    ) {

        return (root, query, cb) -> {

            if (from == null) {
                return cb.conjunction();
            }

            return cb.greaterThanOrEqualTo(
                    root.get("createdAt"),
                    from
            );

        };
    }
    public static Specification<Subscriber> createdBefore(
            Instant to
    ) {

        return (root, query, cb) -> {

            if (to == null) {
                return cb.conjunction();
            }

            return cb.lessThanOrEqualTo(
                    root.get("createdAt"),
                    to
            );

        };
    }
    public static Specification<Subscriber> hasSkills(
            List<Long> skillIds
    ) {

        return (root, query, cb) -> {

            if(skillIds == null || skillIds.isEmpty()) {
                return cb.conjunction();
            }

            query.distinct(true);

            Join<Subscriber, SubscriberSkill> subscriberSkill =
                    root.join("subscriberSkills");

            Join<SubscriberSkill, Skill> skill =
                    subscriberSkill.join("skill");


            return skill.get("id").in(skillIds);
        };
    }



















}
