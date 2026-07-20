package com.webjob.application.dto.Request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class PasswordRequest {
    @NotBlank(message = "Mật khẩu không được để trống")
    private String password;
}
