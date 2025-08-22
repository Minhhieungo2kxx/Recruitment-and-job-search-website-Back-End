package com.webjob.application.Models.Response.Messensage;

import com.webjob.application.Models.Response.ApiResponse;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApiResponseSocket <T>{
    private boolean success;
    private String message;
    private T data;
    private Instant timestamp;

    public static <T> ApiResponseSocket<T> success(T data) {
        return new ApiResponseSocket<>(true, "Thành công", data, Instant.now());
    }

    public static <T> ApiResponseSocket<T> success(T data, String message) {
        return new ApiResponseSocket<>(true, message, data, Instant.now());
    }

    public static <T> ApiResponseSocket<T> error(String message) {
        return new ApiResponseSocket<>(false, message, null, Instant.now());
    }

    public static <T> ApiResponseSocket<T> error(String message, T data) {
        return new ApiResponseSocket<>(false, message, data, Instant.now());
    }

}
