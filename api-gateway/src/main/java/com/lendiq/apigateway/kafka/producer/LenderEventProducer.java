package com.lendiq.apigateway.kafka.producer;

import com.lendiq.apigateway.kafka.event.LenderChangedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class LenderEventProducer {

    private final KafkaTemplate<String, LenderChangedEvent> kafkaTemplate;

    @Value("${kafka.topic.lender-events}")
    private String topic;

    public void publish(LenderChangedEvent event) {
        kafkaTemplate.send(topic, event.lenderId().toString(), event)
            .whenComplete((result, ex) -> {
                if (ex != null) {
                    log.error("Failed to publish lender event [lenderId={}]",
                        event.lenderId(), ex);
                } else {
                    log.info("Published lender event [lenderId={}, type={}, partition={}]",
                        event.lenderId(), event.changeType(),
                        result.getRecordMetadata().partition());
                }
            });
    }
}
