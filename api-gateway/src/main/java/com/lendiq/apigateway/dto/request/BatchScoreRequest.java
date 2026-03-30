package com.lendiq.apigateway.dto.request;

import jakarta.validation.constraints.*;
import java.util.List;
import java.util.UUID;

public record BatchScoreRequest(
    @NotNull @Size(min = 1, max = 1000)
    List<UUID> applicationIds,

    @Pattern(regexp = "https?://.+")
    String callbackwebhookUrl

) {}
