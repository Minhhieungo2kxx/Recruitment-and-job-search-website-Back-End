package com.webjob.application.event;

import lombok.*;


@AllArgsConstructor
@Builder
@Getter
@Setter
public class ResumeFileDeletedEvent {
    private String publicId;

    private String resourceType;

}
