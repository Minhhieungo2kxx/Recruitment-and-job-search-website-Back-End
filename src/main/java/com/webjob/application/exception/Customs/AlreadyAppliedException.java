package com.webjob.application.exception.Customs;

public class AlreadyAppliedException extends RuntimeException{
    public AlreadyAppliedException(String message) {
        super(message);
    }
}
