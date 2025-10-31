package com.webjob.application.Service.Specification;

import com.webjob.application.Model.Entity.Resume;
import com.webjob.application.Model.Enums.ResumeStatus;
import org.springframework.data.jpa.domain.Specification;

public class ResumeSpecification {


    public static Specification<Resume> hasUserId(Long userId) {
        return userId == null
                ? null
                : (root, query, cb) -> cb.equal(root.get("user").get("id"), userId);
    }

    public static Specification<Resume> hasStatus(ResumeStatus status) {
        return status == null
                ? null
                : (root, query, cb) -> cb.equal(root.get("status"), status);
    }

}
