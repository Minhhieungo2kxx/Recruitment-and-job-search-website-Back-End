package com.webjob.application.dto.Request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class IndustryRequest {
    @NotBlank(message = "Tên ngành nghề không được để trống")
    @Size(max = 255, message = "Tên ngành nghề không được vượt quá 255 ký tự")
    private String name;

    private Boolean active;
}
