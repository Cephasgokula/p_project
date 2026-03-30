package com.lendiq.apigateway.exception;

public class VelocityFraudException extends LendIQException {
    public VelocityFraudException() {
        super("Velocity fraud threshold exceeded", "VELOCITY_EXCEEDED");
    }
}
