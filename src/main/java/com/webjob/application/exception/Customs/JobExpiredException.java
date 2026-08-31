package com.webjob.application.exception.Customs;

public class JobExpiredException extends RuntimeException{


    public JobExpiredException(String message) {
        super(message);
    }

    public JobExpiredException(String message,Throwable throwable) {

        super(message,throwable);
    }
}
