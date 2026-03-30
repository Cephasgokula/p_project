package com.lendiq.apigateway.exception;

public class ScoringServiceUnavailableException extends LendIQException {
    public ScoringServiceUnavailableException(String message) {
        super(message, "SCORING_UNAVAILABLE");
    }
}
