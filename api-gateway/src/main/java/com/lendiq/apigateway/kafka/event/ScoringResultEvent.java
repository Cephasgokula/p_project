package com.lendiq.apigateway.kafka.event;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record ScoringResultEvent(
    UUID eventId,
    UUID applicationId,
    UUID applicantId,

    double dtScore,
    double mlScore,
    double fairnessScore,
    double finalScore,

    Double fraudProbability,
    boolean gnnTimedOut,

    String outcome,

    UUID assignedLenderId,
    String assignedLenderName,

    String modelVersion,
    Map<String, Double> shapValues,
    List<String> decisionPath,

    int processingMs,
    Instant decidedAt
) {}
