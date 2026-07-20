package com.webjob.application.dto.Response;

import com.webjob.application.enums.SubscriberStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubscriberResponse {
    private Long id;

    private String name;

    private String email;

    private String phoneNumber;

    private String description;

    private SubscriberStatus status;

    private boolean subscribed;

    private List<SkillResponse> skills;

    private Instant createdAt;

    private Instant updatedAt;

    private String createdBy;

    private String updatedBy;

}
