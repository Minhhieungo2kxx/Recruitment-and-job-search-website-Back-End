package com.webjob.application.event.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;

@Data
@Builder
@AllArgsConstructor

public class JobCreatedEvent {

    private Long companyId;
    private String companyName;
    private String jobName;
    private String local;
    private Double SalaryMin;
    private Double SalaryMax;
    private Long userId;
    private Long jobId;


}
