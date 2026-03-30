package com.lendiq.apigateway.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaConfig {

    @Value("${kafka.topic.loan-apps}")
    private String loanAppsTopic;

    @Value("${kafka.topic.fraud-results}")
    private String fraudResultsTopic;

    @Value("${kafka.topic.scoring-results}")
    private String scoringResultsTopic;

    @Value("${kafka.topic.lender-events}")
    private String lenderEventsTopic;

    @Bean
    public NewTopic loanAppsTopic() {
        return TopicBuilder.name(loanAppsTopic)
            .partitions(12)
            .replicas(1)
            .build();
    }

    @Bean
    public NewTopic fraudResultsTopic() {
        return TopicBuilder.name(fraudResultsTopic)
            .partitions(6)
            .replicas(1)
            .build();
    }

    @Bean
    public NewTopic scoringResultsTopic() {
        return TopicBuilder.name(scoringResultsTopic)
            .partitions(6)
            .replicas(1)
            .build();
    }

    @Bean
    public NewTopic lenderEventsTopic() {
        return TopicBuilder.name(lenderEventsTopic)
            .partitions(3)
            .replicas(1)
            .build();
    }
}
