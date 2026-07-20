package com.webjob.application.dto.Response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PermissionResponse {
    private Long id;

    private String code;

    private String name;
}
