package com.webjob.application.Exception.Customs;

public class PaymentExpiredException extends RuntimeException{
    public PaymentExpiredException(String message) {
        super(message);
    }
}
