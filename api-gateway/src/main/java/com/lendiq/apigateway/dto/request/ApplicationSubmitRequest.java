package com.lendiq.apigateway.dto.request;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record ApplicationSubmitRequest(

    @NotNull(message = "applicant_id is required")
    UUID applicantId,

    @NotNull(message = "amount is required")
    @DecimalMin(value = "1000", message = "amount must be at least 1000")
    @DecimalMax(value = "10000000", message = "amount must not exceed 10000000")
    BigDecimal amount,

    @NotNull(message = "term_months is required")
    @Min(value = 3, message = "term_months must be at least 3")
    @Max(value = 360, message = "term_months must not exceed 360")
    Integer termMonths,

    @NotBlank(message = "purpose is required")
    @Pattern(regexp = "home|vehicle|personal|business", message = "purpose must be one of: home, vehicle, personal, business")
    String purpose,

    @NotBlank(message = "deviceFingerprint is required")
    @Size(max = 128, message = "deviceFingerprint must not exceed 128 characters")
    String deviceFingerprint,

    @NotNull(message = "consentTimestamp is required")
    @PastOrPresent(message = "consentTimestamp must be in the past or present")
    Instant consentTimestamp

) {}
