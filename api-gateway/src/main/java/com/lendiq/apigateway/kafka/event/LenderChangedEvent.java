package com.lendiq.apigateway.kafka.event;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record LenderChangedEvent(
    UUID eventId,
    UUID lenderId,
    String changeType,

    BigDecimal incomeMin,
    BigDecimal incomeMax,
    Integer ageMin,
    Integer ageMax,
    BigDecimal scoreThreshold,
    BigDecimal maxLoanAmount,
    boolean active,

    Instant changedAt
) {}
