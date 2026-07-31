package com.webjob.application.event.dto;

import com.webjob.application.models.Entity.Job;
import com.webjob.application.models.Entity.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class JobAppliedNotificationEvent {
    private Long applicationId;

    private Long hrId;

    private Long candidateId;

    private String nameJob;
    private String nameCompany;
}
