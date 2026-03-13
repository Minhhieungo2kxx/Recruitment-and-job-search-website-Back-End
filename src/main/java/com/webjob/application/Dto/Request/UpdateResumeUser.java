package com.webjob.application.Dto.Request;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UpdateResumeUser {
    @NotBlank
    private String url;

    @NotBlank
    private String publicId;

    @NotBlank
    private String resourceType;
}
