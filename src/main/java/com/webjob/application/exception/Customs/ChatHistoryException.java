package com.webjob.application.exception.Customs;

public class ChatHistoryException extends RuntimeException{

    public ChatHistoryException(String message) {

        super(message);
    }
    public ChatHistoryException(String message,Throwable cause) {

        super(message,cause);
    }


}
