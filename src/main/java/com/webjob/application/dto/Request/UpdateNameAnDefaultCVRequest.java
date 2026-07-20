package com.webjob.application.dto.Request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UpdateNameAnDefaultCVRequest {
    @NotBlank(message = "Tên CV không được để trống")
    private String name;

    private Boolean isDefault=false;
}
