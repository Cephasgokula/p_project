package com.lendiq.apigateway.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import java.util.List;

@ConfigurationProperties(prefix = "lendiq")
public record AppProperties(
    Scoring scoring,
    Fraud fraud,
    Ml ml,
    Security security
) {
    public record Scoring(
        double dtWeight,
        double mlWeight,
        double fairnessWeight,
        double approveThreshold,
        double referThreshold,
        double fraudHardBlock
    ) {}

    public record Fraud(
        int velocityThreshold,
        int velocityWindowSecs,
        long gnnTimeoutMs
    ) {}

    public record Ml(
        String serviceHost,
        int grpcTimeoutMs,
        String modelBucket,
        String championPath
    ) {}

    public record Security(
        String jwtSecret,
        int jwtExpiryHours,
        int refreshExpiryDays,
        List<String> adminCidrs
    ) {}
}
