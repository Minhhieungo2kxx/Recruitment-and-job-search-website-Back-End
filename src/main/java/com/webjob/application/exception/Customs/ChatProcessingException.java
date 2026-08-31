package com.webjob.application.exception.Customs;

public class ChatProcessingException extends RuntimeException{
    public ChatProcessingException(String message) {
        super(message);
    }
    public ChatProcessingException(String message,Throwable throwable) {

        super(message,throwable);
    }
}
