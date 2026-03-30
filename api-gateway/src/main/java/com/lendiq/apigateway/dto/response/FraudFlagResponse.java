package com.lendiq.apigateway.dto.response;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record FraudFlagResponse(
    UUID id,
    UUID applicantId,
    String eventType,
    Integer windowCount,
    String ringId,
    BigDecimal fraudProbability,
    boolean resolved,
    OffsetDateTime detectedAt
) {}
