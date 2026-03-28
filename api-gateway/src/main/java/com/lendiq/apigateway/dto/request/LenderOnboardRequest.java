package com.lendiq.apigateway.dto.request;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public record LenderOnboardRequest(

    @NotBlank @Size(max = 200)
    String name,

    @NotNull @DecimalMin("0") BigDecimal incomeMin,
    @NotNull BigDecimal incomeMax,

    @NotNull @Min(18) @Max(70) Integer ageMin,
    @NotNull @Min(18) @Max(80) Integer ageMax,

    @NotNull @DecimalMin("300") @DecimalMax("1000") 
    BigDecimal scoreThreshold,

    @NotNull @DecimalMin("1000")
    BigDecimal maxLoanAmount,

    @NotBlank @Pattern(regexp = "https?://.+")
    String webhookUrl
) {}
