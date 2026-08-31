package com.webjob.application.exception.Customs;

public class ForbiddenException extends RuntimeException{
    public ForbiddenException(String message) {
        super(message);
    }

    public ForbiddenException(String message,Throwable throwable) {

        super(message,throwable);
    }
}
