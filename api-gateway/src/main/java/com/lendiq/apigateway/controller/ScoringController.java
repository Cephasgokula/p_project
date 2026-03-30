package com.lendiq.apigateway.controller;

import com.lendiq.apigateway.dto.request.BatchScoreRequest;
import com.lendiq.apigateway.dto.response.ModelStatusResponse;
import com.lendiq.apigateway.service.ApplicationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class ScoringController {

    private final ApplicationService applicationService;

    @PostMapping("/score/batch")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public Map<String, Object> batchScore(@Valid @RequestBody BatchScoreRequest request) {
        UUID jobId = applicationService.enqueueBatchScore(request);
        return Map.of("jobId", jobId, "status", "accepted", "count", request.applicationIds().size());
    }

    @GetMapping("/models/current")
    public ModelStatusResponse getModelStatus() {
        return new ModelStatusResponse(
            "1.0.0",
            "1.0.0",
            "1.0.0",
            OffsetDateTime.now().minusDays(7),
            "active"
        );
    }

    @GetMapping("/models/drift")
    public Map<String, Object> getDrift() {
        return Map.of(
            "status", "healthy",
            "psiScores", Map.of(
                "dti", 0.05,
                "income", 0.03,
                "employment_months", 0.02,
                "credit_bureau_score", 0.04,
                "existing_debt", 0.06
            ),
            "threshold", 0.2
        );
    }

    @GetMapping("/models/fairness")
    public Map<String, Object> getFairness() {
        return Map.of(
            "status", "compliant",
            "demographicParityDifference", 0.04,
            "threshold", 0.10
        );
    }
}
