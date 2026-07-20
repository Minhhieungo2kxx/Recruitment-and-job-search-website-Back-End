package com.webjob.application.dto.Response;

import com.webjob.application.enums.SkillStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor

public class SkillResponse {
    private Long id;

    private String name;

    private String description;

    private SkillStatus status;

    private Instant createdAt;

    private String createdBy;


}
