package com.webjob.application.exception.Customs;

public class PaymentExpiredException extends RuntimeException{
    public PaymentExpiredException(String message) {
        super(message);
    }

    public PaymentExpiredException(String message,Throwable throwable) {

        super(message,throwable);
    }
}
