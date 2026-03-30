package com.lendiq.apigateway.service;

import com.lendiq.apigateway.config.AppProperties;
import com.lendiq.apigateway.dsa.*;
import com.lendiq.apigateway.entity.*;
import com.lendiq.apigateway.kafka.event.LoanApplicationEvent;
import com.lendiq.apigateway.kafka.event.ScoringResultEvent;
import com.lendiq.apigateway.kafka.producer.ScoringResultProducer;
import com.lendiq.apigateway.repository.ApplicationRepository;
import com.lendiq.apigateway.repository.DecisionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class ScoringOrchestrationServiceImpl implements ScoringOrchestrationService {

    private final DecisionTree decisionTree;
    private final IntervalTree intervalTree;
    private final AppProperties appProperties;
    private final ScoringResultProducer scoringResultProducer;
    private final ApplicationRepository applicationRepository;
    private final DecisionRepository decisionRepository;
    private final FraudService fraudService;

    // Temporary store for GNN results (async join, max 50ms wait)
    private final ConcurrentHashMap<UUID, Double> gnnResults = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, String> gnnRingIds = new ConcurrentHashMap<>();

    @Override
    @Transactional
    public Decision orchestrate(LoanApplicationEvent event) {
        long startTime = System.currentTimeMillis();

        // 1. Decision Tree scoring
        Map<String, Double> features = buildFeatureMap(event);
        DecisionTree.DecisionResult dtResult = decisionTree.score(features);
        double dtScore = dtResult.score();

        // 2. ML score (simulated — in production, this calls gRPC to Python ML service)
        double mlScore = simulateMlScore(event);

        // 3. Fairness-adjusted score (simulated — in production, calls AIF360 service)
        double fairnessScore = simulateFairnessScore(mlScore);

        // 4. Aggregate scores
        double finalScore = aggregateScore(dtScore, mlScore, fairnessScore);

        // 5. Wait for GNN fraud result (max 50ms)
        Double fraudProb = waitForGnnResult(event.applicationId());

        // 6. Determine outcome
        String outcome = determineOutcome(finalScore, fraudProb);

        // 7. Route to best lender using Interval Tree + Red-Black Tree
        Lender bestLender = null;
        if (!"DECLINE".equals(outcome)) {
            bestLender = routeToBestLender(event, BigDecimal.valueOf(finalScore));
        }

        // 8. Record fraud event if GNN detected fraud ring
        String ringId = gnnRingIds.remove(event.applicationId());
        if (fraudProb != null && fraudProb > 0.5) {
            fraudService.recordGnnFraudEvent(event.applicantId(), fraudProb, ringId);
        }

        int processingMs = (int) (System.currentTimeMillis() - startTime);

        // 9. Build and persist decision
        Application application = applicationRepository.findById(event.applicationId())
            .orElse(null);

        Decision decision = Decision.builder()
            .application(application)
            .dtScore(BigDecimal.valueOf(dtScore))
            .mlScore(BigDecimal.valueOf(mlScore))
            .fairnessScore(BigDecimal.valueOf(fairnessScore))
            .finalScore(BigDecimal.valueOf(finalScore))
            .fraudProb(fraudProb != null ? BigDecimal.valueOf(fraudProb) : null)
            .outcome(outcome)
            .lender(bestLender)
            .modelVersion("1.0.0")
            .shapJson(dtResult.shapValues().toString())
            .decisionPath(dtResult.decisionPath().toArray(new String[0]))
            .processingMs(processingMs)
            .build();

        decision = decisionRepository.save(decision);

        // Update application status
        if (application != null) {
            application.setStatus(outcome.toLowerCase());
            applicationRepository.save(application);
        }

        // 10. Publish scoring result event
        ScoringResultEvent resultEvent = new ScoringResultEvent(
            UUID.randomUUID(),
            event.applicationId(),
            event.applicantId(),
            dtScore, mlScore, fairnessScore, finalScore,
            fraudProb,
            fraudProb == null,
            outcome,
            bestLender != null ? bestLender.getId() : null,
            bestLender != null ? bestLender.getName() : null,
            "1.0.0",
            dtResult.shapValues(),
            dtResult.decisionPath(),
            processingMs,
            Instant.now()
        );
        scoringResultProducer.publish(resultEvent);

        log.info("Scoring complete [appId={}, outcome={}, score={}, latency={}ms]",
            event.applicationId(), outcome, finalScore, processingMs);

        return decision;
    }

    @Override
    public void handleGnnResult(UUID applicationId, double fraudProbability, String ringId) {
        gnnResults.put(applicationId, fraudProbability);
        if (ringId != null) {
            gnnRingIds.put(applicationId, ringId);
        }
    }

    @Override
    public double aggregateScore(double dtScore, double mlScore, double fairnessScore) {
        AppProperties.Scoring scoring = appProperties.scoring();
        return (scoring.dtWeight() * dtScore)
             + (scoring.mlWeight() * mlScore)
             + (scoring.fairnessWeight() * fairnessScore);
    }

    @Override
    public String determineOutcome(double finalScore, Double fraudProbability) {
        AppProperties.Scoring scoring = appProperties.scoring();

        // Hard block if fraud probability exceeds threshold
        if (fraudProbability != null && fraudProbability > scoring.fraudHardBlock()) {
            return "DECLINE";
        }

        if (finalScore >= scoring.approveThreshold()) return "APPROVE";
        if (finalScore >= scoring.referThreshold()) return "REFER";
        return "DECLINE";
    }

    private Map<String, Double> buildFeatureMap(LoanApplicationEvent event) {
        Map<String, Double> features = new LinkedHashMap<>();
        features.put("dti", event.dtiRatio() != null ? event.dtiRatio().doubleValue() : 0.0);
        features.put("employment_months", (double) event.employmentMonths());
        features.put("credit_bureau_score", (double) event.creditBureauScore());
        features.put("monthly_income", event.monthlyIncome() != null ? event.monthlyIncome().doubleValue() : 0.0);
        features.put("existing_debt", event.existingDebt() != null ? event.existingDebt().doubleValue() : 0.0);
        return features;
    }

    private double simulateMlScore(LoanApplicationEvent event) {
        // Simulated ML score — in production, this calls the Python gRPC service
        double base = 500.0;
        if (event.creditBureauScore() > 700) base += 150;
        else if (event.creditBureauScore() > 600) base += 80;
        if (event.dtiRatio() != null && event.dtiRatio().doubleValue() < 0.3) base += 100;
        if (event.employmentMonths() > 36) base += 60;
        return Math.min(1000, Math.max(0, base));
    }

    private double simulateFairnessScore(double mlScore) {
        // Simulated fairness adjustment — in production, calls AIF360
        return mlScore * 0.95 + 25; // slight adjustment
    }

    private Double waitForGnnResult(UUID applicationId) {
        long timeoutMs = appProperties.fraud().gnnTimeoutMs();
        long deadline = System.currentTimeMillis() + timeoutMs;

        while (System.currentTimeMillis() < deadline) {
            Double result = gnnResults.remove(applicationId);
            if (result != null) return result;
            try {
                Thread.sleep(5);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        return null; // GNN timed out
    }

    private Lender routeToBestLender(LoanApplicationEvent event, BigDecimal finalScore) {
        BigDecimal income = event.monthlyIncome() != null ? event.monthlyIncome() : BigDecimal.ZERO;
        int age = event.ageYears();

        List<Lender> eligible = intervalTree.queryEligible(
            income, age, finalScore, event.requestedAmount()
        );

        if (eligible.isEmpty()) return null;

        // Use Red-Black Tree to sort by composite match score
        RedBlackTree rbTree = new RedBlackTree();
        for (Lender lender : eligible) {
            double matchScore = computeMatchScore(lender, finalScore.doubleValue());
            rbTree.insert(lender.getId(), lender.getName(), matchScore);
        }

        RedBlackTree.RBNode best = rbTree.getMax();
        if (best == null) return null;

        return eligible.stream()
            .filter(l -> l.getId().equals(best.lenderId))
            .findFirst()
            .orElse(null);
    }

    private double computeMatchScore(Lender lender, double finalScore) {
        // 70% credit score compatibility + 20% acceptance rate history + 10% fee competitiveness
        double scoreCompat = finalScore / lender.getScoreThreshold().doubleValue();
        return 0.70 * Math.min(scoreCompat, 1.5) * 100
             + 0.20 * 75 // placeholder for acceptance rate
             + 0.10 * 80; // placeholder for fee competitiveness
    }
}
