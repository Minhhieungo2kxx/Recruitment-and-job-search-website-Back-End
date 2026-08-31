package com.webjob.application.exception.Customs;

public class CompanyAlreadyExistsException extends RuntimeException{
    public CompanyAlreadyExistsException(String message) {
        super(message);
    }

    public CompanyAlreadyExistsException(String message,Throwable throwable) {

        super(message,throwable);
    }
}
