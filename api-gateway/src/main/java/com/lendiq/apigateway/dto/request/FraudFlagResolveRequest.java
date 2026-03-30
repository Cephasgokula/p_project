package com.lendiq.apigateway.dto.request;

import jakarta.validation.constraints.*;

public record FraudFlagResolveRequest(

    @NotBlank @Size(min = 10,max = 20000)
    String reviewerNotes,

    @NotBlank
    String reviewBy
) {}