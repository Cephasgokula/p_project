package com.lendiq.apigateway.kafka.producer;

import com.lendiq.apigateway.kafka.event.LoanApplicationEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ApplicationEventProducer {

    private final KafkaTemplate<String, LoanApplicationEvent> kafkaTemplate;

    @Value("${kafka.topic.loan-apps}")
    private String topic;

    public void publish(LoanApplicationEvent event) {
        kafkaTemplate.send(topic, event.applicantId().toString(), event)
            .whenComplete((result, ex) -> {
                if (ex != null) {
                    log.error("Failed to publish application event [id={}]",
                        event.applicationId(), ex);
                } else {
                    log.info("Published loan-app event [id={}, partition={}, offset={}]",
                        event.applicationId(),
                        result.getRecordMetadata().partition(),
                        result.getRecordMetadata().offset());
                }
            });
    }
}
