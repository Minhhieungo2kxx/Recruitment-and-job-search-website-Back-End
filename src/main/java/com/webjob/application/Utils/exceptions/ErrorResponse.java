package com.webjob.application.Utils.exceptions;



import lombok.*;

import java.time.LocalDateTime;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ErrorResponse<T> {
    private int status;
    private String message;
    private LocalDateTime timestamp;
    private Map<String, String> errors;
    T data;


}
