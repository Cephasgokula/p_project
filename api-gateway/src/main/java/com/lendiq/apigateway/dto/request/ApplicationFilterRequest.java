package com.lendiq.apigateway.dto.request;

import java.time.Instant;
import java.util.UUID;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Pattern;

public record ApplicationFilterRequest(

    UUID applicantId,

    @Pattern(regexp = "pending|approved|declined|referred")
    String status,

    @PastOrPresent Instant from,
    Instant to,

    @Pattern(regexp = "api|web|mobile|partner")
    String channel,

    @Min(0) int page,
    @Min(1) @Max(100) int size

) {}
