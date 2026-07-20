package com.webjob.application.exception.Customs;

public class RedisUnavailableException extends RuntimeException{
    public RedisUnavailableException(String message) {
        super(message);
    }
}
