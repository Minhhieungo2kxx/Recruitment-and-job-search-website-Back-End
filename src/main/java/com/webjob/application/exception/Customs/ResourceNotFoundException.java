package com.webjob.application.exception.Customs;

public class ResourceNotFoundException extends RuntimeException{
    public ResourceNotFoundException(String message) {
        super(message);
    }

    public ResourceNotFoundException(String message,Throwable throwable) {
        super(message,throwable);
    }
}
