package com.lendiq.apigateway.exception;

public class InsufficientPermissionException extends LendIQException {
    public InsufficientPermissionException(String message) {
        super(message, "FORBIDDEN");
    }
}
