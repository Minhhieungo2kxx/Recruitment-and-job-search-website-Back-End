package com.webjob.application.exception.Customs;

public class CvProcessingException extends RuntimeException{
    public CvProcessingException(String message) {
        super(message);
    }

    public CvProcessingException(String message, Throwable cause) {
        super(message, cause);
    }
}
