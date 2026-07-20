package com.webjob.application.dto.Request;

import com.webjob.application.enums.SkillStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SkillSearchRequest {
    private String keyword;

    private SkillStatus status;

    private String createdBy;

    private Instant fromDate;

    private Instant toDate;
}
