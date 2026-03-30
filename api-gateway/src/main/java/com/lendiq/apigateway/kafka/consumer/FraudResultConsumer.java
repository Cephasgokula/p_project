package com.lendiq.apigateway.kafka.consumer;

import com.lendiq.apigateway.kafka.event.FraudResultEvent;
import com.lendiq.apigateway.service.ScoringOrchestrationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class FraudResultConsumer {

    private final ScoringOrchestrationService scoringOrchestrationService;

    @KafkaListener(
        topics = "${kafka.topic.fraud-results}",
        groupId = "lendiq-fraud-consumer",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void consume(FraudResultEvent event) {
        log.info("Received GNN fraud result [appId={}, fraudProb={}]",
            event.applicationId(), event.fraudProbability());

        scoringOrchestrationService.handleGnnResult(
            event.applicationId(),
            event.fraudProbability(),
            event.ringId()
        );
    }
}
