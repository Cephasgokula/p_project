package com.lendiq.apigateway.dto.response;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.List;

public record ApplicationSubmitResponse(
    UUID applicationId,
    String status,
    String outcome,
    BigDecimal finalScore,
    LenderSummary lender,
    List<String> decisionPath,
    Integer processingMs
) {
    public record LenderSummary(UUID id, String name) {}
}
