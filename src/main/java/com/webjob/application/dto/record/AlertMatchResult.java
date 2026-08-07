package com.webjob.application.dto.record;

import com.webjob.application.models.Entity.Job;

public record AlertMatchResult(
        Job job,
        Integer rawAlertScore
) {
}
