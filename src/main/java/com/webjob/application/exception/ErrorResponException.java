package com.webjob.application.exception;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ErrorResponException<T> {
    private int statusCode;
    private String message;

    private LocalDateTime timestamp;
    private String error;

    T data;

}
