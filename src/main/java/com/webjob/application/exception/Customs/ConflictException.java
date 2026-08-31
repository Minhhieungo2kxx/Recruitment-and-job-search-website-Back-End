package com.webjob.application.exception.Customs;

public class ConflictException extends RuntimeException{
    public ConflictException(String message) {
        super(message);
    }

    public ConflictException(String message,Throwable throwable) {

        super(message,throwable);
    }
}
