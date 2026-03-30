package com.lendiq.apigateway.kafka.event;

import java.time.Instant;
import java.util.UUID;

public record FraudResultEvent(
    UUID eventId,
    UUID applicationId,
    UUID applicantId,

    double fraudProbability,
    String ringId,
    boolean isFraudRing,
    int ringSize,

    long inferenceMs,
    Instant processedAt
) {}
