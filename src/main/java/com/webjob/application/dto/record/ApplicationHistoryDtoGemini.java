package com.webjob.application.dto.record;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApplicationHistoryDtoGemini(
        Long jobId,
        String jobName,
        String status,
        String hrNote,
        String appliedAt

) {
}
