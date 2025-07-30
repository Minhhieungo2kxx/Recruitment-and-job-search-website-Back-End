package com.webjob.application.Utils.exceptions;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ErrorResponException<T> {
    private int statusCode;
    private String message;
    private LocalDateTime timestamp;
    private String error;
    T data;

}
