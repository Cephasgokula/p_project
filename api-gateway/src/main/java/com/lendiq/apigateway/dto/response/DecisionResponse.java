package com.lendiq.apigateway.dto.response;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record DecisionResponse(
    UUID id,
    UUID applicationId,
    BigDecimal dtScore,
    BigDecimal mlScore,
    BigDecimal fairnessScore,
    BigDecimal finalScore,
    BigDecimal fraudProbability,
    String outcome,
    String modelVersion,
    Map<String, Double> shapValues,
    List<String> decisionPath,
    LenderInfo lender,
    Integer processingMs,
    OffsetDateTime decidedAt
) {
    public record LenderInfo(UUID id, String name) {}
}
