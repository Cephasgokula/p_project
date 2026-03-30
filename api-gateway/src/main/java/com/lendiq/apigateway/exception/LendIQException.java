package com.lendiq.apigateway.exception;

public class LendIQException extends RuntimeException {
    private final String errorCode;

    public LendIQException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
