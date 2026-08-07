package com.webjob.application.dto.Response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@AllArgsConstructor
@Builder
public class ApplicationSummaryDTO {
    private Long total;
    private Long pending;
    private Long reviewing;
    private Long interviewing;
    private Long offered;
    private Long hired;
    private Long rejected;
    private Instant latestAppliedAt;
}
