package com.webjob.application.service.Specification;

import com.webjob.application.models.Entity.Application;
import com.webjob.application.enums.ResumeStatus;
import org.springframework.data.jpa.domain.Specification;

public class ApplicationSpecification {


    public static Specification<Application> hasUserId(Long userId) {
        return (root, query, cb) ->
                cb.equal(root.get("user").get("id"), userId);
    }

    public static Specification<Application> hasStatus(ResumeStatus status) {

        if (status == null) {
            return null;
        }

        return (root, query, cb) ->
                cb.equal(root.get("status"), status);
    }

}
