package com.lendiq.apigateway.dto.request;

import java.math.BigDecimal;

public record LenderRulesUpdateRequest(
    BigDecimal incomeMin,
    BigDecimal incomeMax,

    Integer ageMin,
    Integer ageMax,

    BigDecimal scoreThreshold,
    BigDecimal maxLoanAmount,

    String webhookUrl

) {}