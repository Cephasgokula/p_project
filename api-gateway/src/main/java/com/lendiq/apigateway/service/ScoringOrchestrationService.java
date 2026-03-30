package com.lendiq.apigateway.service;

import com.lendiq.apigateway.entity.Decision;
import com.lendiq.apigateway.kafka.event.LoanApplicationEvent;

import java.util.UUID;

public interface ScoringOrchestrationService {

    Decision orchestrate(LoanApplicationEvent event);

    void handleGnnResult(UUID applicationId, double fraudProbability, String ringId);

    double aggregateScore(double dtScore, double mlScore, double fairnessScore);

    String determineOutcome(double finalScore, Double fraudProbability);
}
