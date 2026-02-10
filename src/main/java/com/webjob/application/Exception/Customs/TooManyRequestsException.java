package com.webjob.application.Exception.Customs;

public class TooManyRequestsException extends RuntimeException{
    private final long retryAfter;
    public TooManyRequestsException(String message, long retryAfter) {
        super(message);
        this.retryAfter = retryAfter;
    }
    public long getRetryAfter() {
        return retryAfter;
    }


}

//1. Spring chỉ xử lý những exception "ứng dụng" (RuntimeException và các
//exception custom kế thừa từ nó)