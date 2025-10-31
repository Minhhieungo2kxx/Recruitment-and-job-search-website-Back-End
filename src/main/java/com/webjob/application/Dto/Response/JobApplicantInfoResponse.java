package com.webjob.application.Dto.Response;

import com.webjob.application.Model.Enums.CompetitionLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class JobApplicantInfoResponse {
    private Long jobId;
    private String jobName;
    private CompetitionLevel competitionLevel;
    private int appliedCount;
    private boolean hasPaidAccess;
    private String message;
}
