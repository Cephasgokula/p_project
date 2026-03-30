package com.lendiq.apigateway.dto.request;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public record ApplicantRegisterRequest(
    @NotBlank String fullName,
    @NotBlank @Email String email,
    @NotBlank @Size(min = 6) String password,
    @NotBlank String panNumber,
    @NotNull @DecimalMin("0") BigDecimal income,
    @NotNull @Min(18) Integer age,
    @NotNull @Min(0) Integer employmentMonths,
    @DecimalMin("0") BigDecimal existingDebt,
    @Min(300) @Max(900) Integer creditBureauScore,
    String deviceFingerprint,
    String ipAddress
) {}
