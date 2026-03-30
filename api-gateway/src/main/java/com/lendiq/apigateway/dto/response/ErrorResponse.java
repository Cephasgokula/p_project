package com.lendiq.apigateway.dto.response;

import java.time.Instant;

public record ErrorResponse(
    String errorCode,
    String message,
    String traceId,
    Instant timestamp
) {}
