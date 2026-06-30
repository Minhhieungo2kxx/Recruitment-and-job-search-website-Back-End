package com.webjob.application.exception.Customs;

public class ResourceLockedException extends RuntimeException {
    public ResourceLockedException(String message) {
        super(message);
    }
}
