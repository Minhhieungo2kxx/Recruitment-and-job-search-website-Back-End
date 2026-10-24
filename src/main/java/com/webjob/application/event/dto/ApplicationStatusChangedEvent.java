package com.webjob.application.event.dto;

import com.webjob.application.enums.ResumeStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

@Getter
@Builder
@AllArgsConstructor
public class ApplicationStatusChangedEvent {
    private Long applicationId;


    private Long candidateId;


//    private String candidateName;


    private String jobName;


    private String companyName;


    private ResumeStatus oldStatus;


    private ResumeStatus newStatus;


//    private Instant changedAt;
}
