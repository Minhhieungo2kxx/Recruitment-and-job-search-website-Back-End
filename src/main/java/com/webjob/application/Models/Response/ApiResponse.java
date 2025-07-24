package com.webjob.application.Models.Response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ApiResponse <T>{
    private int statusCode;
    private String error;
    private String message;
    private T data;
}
