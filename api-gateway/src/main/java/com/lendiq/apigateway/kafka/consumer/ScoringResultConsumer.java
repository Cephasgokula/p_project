package com.lendiq.apigateway.kafka.consumer;

import com.lendiq.apigateway.entity.Application;
import com.lendiq.apigateway.entity.Decision;
import com.lendiq.apigateway.kafka.event.ScoringResultEvent;
import com.lendiq.apigateway.repository.ApplicationRepository;
import com.lendiq.apigateway.repository.DecisionRepository;
import com.lendiq.apigateway.repository.LenderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Slf4j
@Component
@RequiredArgsConstructor
public class ScoringResultConsumer {

    private final ApplicationRepository applicationRepository;
    private final DecisionRepository decisionRepository;
    private final LenderRepository lenderRepository;

    @KafkaListener(
        topics = "${kafka.topic.scoring-results}",
        groupId = "lendiq-scoring-result-consumer",
        containerFactory = "kafkaListenerContainerFactory"
    )
    @Transactional
    public void consume(ScoringResultEvent event) {
        log.info("Received scoring result [appId={}, outcome={}]",
            event.applicationId(), event.outcome());

        Application application = applicationRepository.findById(event.applicationId())
            .orElse(null);
        if (application == null) {
            log.warn("Application not found for scoring result: {}", event.applicationId());
            return;
        }

        // Update application status
        application.setStatus(event.outcome().toLowerCase());
        applicationRepository.save(application);

        // Persist decision (append-only audit trail)
        Decision decision = Decision.builder()
            .application(application)
            .dtScore(BigDecimal.valueOf(event.dtScore()))
            .mlScore(BigDecimal.valueOf(event.mlScore()))
            .fairnessScore(BigDecimal.valueOf(event.fairnessScore()))
            .finalScore(BigDecimal.valueOf(event.finalScore()))
            .fraudProb(event.fraudProbability() != null
                ? BigDecimal.valueOf(event.fraudProbability()) : null)
            .outcome(event.outcome())
            .modelVersion(event.modelVersion())
            .shapJson("{}")
            .decisionPath(event.decisionPath() != null
                ? event.decisionPath().toArray(new String[0]) : new String[0])
            .processingMs(event.processingMs())
            .build();

        if (event.assignedLenderId() != null) {
            lenderRepository.findById(event.assignedLenderId())
                .ifPresent(decision::setLender);
        }

        decisionRepository.save(decision);
        log.info("Decision persisted [appId={}, outcome={}, score={}]",
            event.applicationId(), event.outcome(), event.finalScore());
    }
}
