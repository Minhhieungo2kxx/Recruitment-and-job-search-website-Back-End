package com.webjob.application.Utils.exceptions;

public class TooManyRequestsException extends RuntimeException{
    public TooManyRequestsException(String message) {
        super(message);
    }

}

//1. Spring chỉ xử lý những exception "ứng dụng" (RuntimeException và các
//exception custom kế thừa từ nó)