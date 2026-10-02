package com.webjob.application.dto.event;

import lombok.*;


@AllArgsConstructor
@Builder
@Getter
@Setter
public class ResumeFileDeletedEvent {
    private String publicId;

    private String resourceType;

}
