package com.lendiq.apigateway.kafka.event;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record LoanApplicationEvent(
    UUID eventId,
    UUID applicationId,
    UUID applicantId,
    long kafkaOffset,

    BigDecimal requestedAmount,
    int termMonths,
    String purpose,

    BigDecimal monthlyIncome,
    int ageYears,
    int employmentMonths,
    BigDecimal existingDebt,
    int creditBureauScore,
    BigDecimal dtiRatio,

    String ipHash,
    String deviceFingerprint,
    boolean velocityFlagged,

    Instant submittedAt,
    String sourceChannel
) {}
