package com.lendiq.apigateway.dto.response;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record ApplicantResponse(
    UUID id,
    String fullName,
    BigDecimal income,
    Integer age,
    Integer employmentMonths,
    BigDecimal existingDebt,
    Integer creditBureauScore,
    OffsetDateTime createdAt
) {}
