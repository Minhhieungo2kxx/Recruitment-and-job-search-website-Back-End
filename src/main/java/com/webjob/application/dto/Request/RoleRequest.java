package com.webjob.application.dto.Request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class RoleRequest {
    @NotBlank
    private String code;

    @NotBlank
    private String displayName;

    @NotBlank
    private String description;

    private boolean active;

    private List<Long> permissionIds;
}
