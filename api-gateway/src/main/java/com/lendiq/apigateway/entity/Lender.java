package com.lendiq.apigateway.entity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "lenders")

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Lender {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Column(name = "income_min",nullable = false,precision = 12,scale = 2)
    private BigDecimal incomeMin;

    @Column(name = "income_max",nullable = false,precision = 12,scale = 2)
    private BigDecimal incomeMax;

    @Column(name = "age_min",nullable = false)
    private Integer ageMin;

    @Column(name = "age_max",nullable = false)
    private Integer ageMax;

    @Column(name = "score_threshold",nullable = false,precision = 6,scale = 2)
    private BigDecimal scoreThreshold;

    @Column(name = "max_loan_amount",nullable = false,precision = 12,scale = 2)
    private BigDecimal maxLoanAmount;

    @Column(name = "webhook_url",nullable = false)
    private String webhookUrl;

    @Builder.Default
    @Column(nullable = false, columnDefinition = "BOOLEAN DEFAULT true")
    private boolean active = true;
}
