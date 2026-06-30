package com.webjob.application.messaging.dto;

import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JobAppliedEvent {
    private String email;

    private String username;

    private String usernameHR;

    private String companyName;

    private String companyLogo;

    private String jobName;

    private BigDecimal salary;

    private String location;

    private Instant startDate;

    private Instant endDate;
}
