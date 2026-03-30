package com.lendiq.apigateway.dto.response;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record AuditTrailResponse(
    UUID applicationId,
    UUID applicantId,
    OffsetDateTime submittedAt,
    String sourceChannel,
    BigDecimal requestedAmount,
    String loanPurpose,
    DecisionResponse decision,
    List<FraudFlagResponse> fraudFlags,
    String kafkaOffset
) {}
