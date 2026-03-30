package com.lendiq.apigateway.dto.response;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record ApplicationDetailResponse(
    UUID id,
    UUID applicantId,
    BigDecimal amount,
    Integer termMonths,
    String purpose,
    String status,
    String sourceChannel,
    OffsetDateTime createdAt,
    DecisionSummary decision
) {
    public record DecisionSummary(
        String outcome,
        BigDecimal finalScore,
        String modelVersion,
        OffsetDateTime decidedAt
    ) {}
}
