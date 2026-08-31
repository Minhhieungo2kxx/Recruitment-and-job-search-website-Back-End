package com.webjob.application.exception.Customs;

public class ResourceLockedException extends RuntimeException {
    public ResourceLockedException(String message) {
        super(message);
    }

    public ResourceLockedException(String message,Throwable throwable) {

        super(message,throwable);
    }
}
