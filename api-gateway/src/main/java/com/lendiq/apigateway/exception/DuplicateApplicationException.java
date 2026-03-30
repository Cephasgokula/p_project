package com.lendiq.apigateway.exception;

public class DuplicateApplicationException extends LendIQException {
    public DuplicateApplicationException(String message) {
        super(message, "DUPLICATE_APPLICATION");
    }
}
