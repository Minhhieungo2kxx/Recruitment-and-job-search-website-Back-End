package com.webjob.application.dto.record;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record SavedJobDtoGemini(
        Long jobId,
        String jobName,
        String companyName,
        String savedAt

) {
}
