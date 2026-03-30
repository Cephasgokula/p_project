package com.lendiq.apigateway.dto.response;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record LenderStatsResponse(
    UUID lenderId,
    String lenderName,
    long totalReferrals,
    long approvals,
    double acceptanceRate,
    BigDecimal avgFinalScore,
    OffsetDateTime periodStart,
    OffsetDateTime periodEnd
) {}
