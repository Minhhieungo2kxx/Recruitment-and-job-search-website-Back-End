package com.webjob.application.dto.record;

import lombok.*;


@AllArgsConstructor
@Builder
@Getter
@Setter
public class ResumeFileDeletedEvent {
    private String publicId;

    private String resourceType;

}
