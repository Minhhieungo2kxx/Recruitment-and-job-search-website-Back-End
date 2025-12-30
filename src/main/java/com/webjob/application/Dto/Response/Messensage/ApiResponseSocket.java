package com.webjob.application.Dto.Response.Messensage;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.http.HttpStatus;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApiResponseSocket <T>{
    private boolean success;
    private String message;
    private T data;
    private Instant timestamp;
    private int statusCode;

    public static <T> ApiResponseSocket<T> success(T data) {
        return new ApiResponseSocket<>(true, "Thành công", data, Instant.now(), HttpStatus.OK.value());
    }

    public static <T> ApiResponseSocket<T> success(T data, String message) {
        return new ApiResponseSocket<>(true, message, data, Instant.now(),HttpStatus.OK.value());
    }

    public static <T> ApiResponseSocket<T> error(String message) {
        return new ApiResponseSocket<>(false, message, null, Instant.now(),HttpStatus.UNAUTHORIZED.value());
    }

    public static <T> ApiResponseSocket<T> error(String message, T data) {
        return new ApiResponseSocket<>(false, message, data, Instant.now(),HttpStatus.UNAUTHORIZED.value());
    }

}
