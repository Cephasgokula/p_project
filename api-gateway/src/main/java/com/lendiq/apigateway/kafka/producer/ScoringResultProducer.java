package com.lendiq.apigateway.kafka.producer;

import com.lendiq.apigateway.kafka.event.ScoringResultEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ScoringResultProducer {

    private final KafkaTemplate<String, ScoringResultEvent> kafkaTemplate;

    @Value("${kafka.topic.scoring-results}")
    private String topic;

    public void publish(ScoringResultEvent event) {
        kafkaTemplate.send(topic, event.applicationId().toString(), event)
            .whenComplete((result, ex) -> {
                if (ex != null) {
                    log.error("Failed to publish scoring result [appId={}]",
                        event.applicationId(), ex);
                } else {
                    log.info("Published scoring result [appId={}, outcome={}]",
                        event.applicationId(), event.outcome());
                }
            });
    }
}
