package com.webjob.application.dto.Request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class IndustryFilter {
    private String keyword;
    private Boolean active;
    private Boolean deleted;
    private Instant createdFrom;
    private Instant createdTo;
}
