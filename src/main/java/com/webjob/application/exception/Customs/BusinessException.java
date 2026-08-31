package com.webjob.application.exception.Customs;

public class BusinessException extends RuntimeException{
    public BusinessException(String message) {
        super(message);
    }

    public BusinessException(String message,Throwable throwable) {

        super(message,throwable);
    }
}
