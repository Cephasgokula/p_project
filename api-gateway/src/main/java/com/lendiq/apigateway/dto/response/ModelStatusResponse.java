package com.lendiq.apigateway.dto.response;

import java.time.OffsetDateTime;

public record ModelStatusResponse(
    String lightgbmVersion,
    String gnnVersion,
    String fairnessVersion,
    OffsetDateTime lastRetrainedAt,
    String status
) {}
