package com.webjob.application.dto.Response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class AdminResumeResponse {
    private Long id;

    private String resumeName;

    private String ownerName;

    private String ownerEmail;

    private Boolean isDefault;

    private Instant createdAt;

    private String url;

    private Long totalApplications;
}
