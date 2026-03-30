package com.lendiq.apigateway.dto.response;

import java.math.BigDecimal;
import java.util.UUID;

public record LenderResponse(
    UUID id,
    String name,
    BigDecimal incomeMin,
    BigDecimal incomeMax,
    Integer ageMin,
    Integer ageMax,
    BigDecimal scoreThreshold,
    BigDecimal maxLoanAmount,
    boolean active
) {}
