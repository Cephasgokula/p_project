package com.lendiq.apigateway.exception;

public class ResourceNotFoundException extends LendIQException {
    public ResourceNotFoundException(String resource, Object id) {
        super(resource + " not found with id: " + id, resource.toUpperCase() + "_NOT_FOUND");
    }
}
