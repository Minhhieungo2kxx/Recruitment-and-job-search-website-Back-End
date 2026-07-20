package com.webjob.application.service.Specification;

import com.webjob.application.enums.SkillStatus;
import com.webjob.application.models.Entity.Skill;
import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;

public class SkillSpecification {
    public static Specification<Skill> hasKeyword(String keyword){

        return (root, query, cb)->{

            if(keyword == null || keyword.isBlank()){
                return null;
            }

            String value = "%" + keyword.toLowerCase() + "%";

            return cb.or(
                    cb.like(cb.lower(root.get("name")), value),
                    cb.like(cb.lower(root.get("description")), value)
            );

        };
    }
    public static Specification<Skill> hasStatus(SkillStatus status){

        return (root, query, cb)->{

            if(status == null){
                return null;
            }

            return cb.equal(root.get("status"), status);

        };

    }
    public static Specification<Skill> createdBy(String createdBy){

        return (root, query, cb)->{

            if(createdBy == null || createdBy.isBlank()){
                return null;
            }

            return cb.equal(root.get("createdBy"), createdBy);

        };

    }
    public static Specification<Skill> createdAfter(Instant from){

        return (root, query, cb)->{

            if(from == null){
                return null;
            }

            return cb.greaterThanOrEqualTo(root.get("createdAt"), from);

        };

    }
    public static Specification<Skill> createdBefore(Instant to){

        return (root, query, cb)->{

            if(to == null){
                return null;
            }

            return cb.lessThanOrEqualTo(root.get("createdAt"), to);

        };

    }

}
