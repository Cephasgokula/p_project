package com.lendiq.apigateway.config;

import com.lendiq.apigateway.dsa.IntervalTree;
import com.lendiq.apigateway.entity.Lender;
import com.lendiq.apigateway.repository.LenderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Slf4j
@Configuration
@RequiredArgsConstructor
@EnableConfigurationProperties(AppProperties.class)
public class IntervalTreeConfig implements ApplicationRunner {

    private final LenderRepository lenderRepository;
    private final IntervalTree intervalTree;

    @Override
    public void run(ApplicationArguments args) {
        List<Lender> activeLenders = lenderRepository.findByActiveTrue();
        intervalTree.rebuild(activeLenders);
        log.info("Interval Tree loaded with {} active lenders", activeLenders.size());
    }
}
