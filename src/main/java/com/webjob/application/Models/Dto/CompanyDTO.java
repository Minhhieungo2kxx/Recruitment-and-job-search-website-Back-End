package com.webjob.application.Models.Dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CompanyDTO {

    @NotBlank(message = "Tên công ty không được để trống")
    @Size(min = 2, max = 255, message = "Tên công ty phải có từ 2 đến 255 ký tự")
    private String name;

    private String description;

    @Size(max = 500, message = "Địa chỉ không được vượt quá 500 ký tự")
    private String address;

    @Size(max = 500, message = "Đường dẫn logo không được vượt quá 500 ký tự")
    private String logo;



}
