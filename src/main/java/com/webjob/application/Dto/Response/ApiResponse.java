package com.webjob.application.Dto.Response;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ApiResponse <T>{
    private int statusCode;
    private String error;
    private String message;
    private T data;
}
