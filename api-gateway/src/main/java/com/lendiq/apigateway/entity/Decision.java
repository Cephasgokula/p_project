package com.lendiq.apigateway.entity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "decisions")

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Decision {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false,nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY,optional = false)
    @JoinColumn(name = "application_id",nullable = false,foreignKey = @ForeignKey(name = "fk_decisions_application"))
    private Application application;

    @Column(name = "dt_score", nullable = false,precision = 6,scale = 2)
    private BigDecimal dtScore;

    @Column(name = "ml_score",nullable = false,precision = 6,scale = 2)
    private BigDecimal mlScore;
    
    @Column(name = "fairness_score",nullable = false,precision = 6,scale = 2)
    private BigDecimal fairnessScore;

    @Column(name = "final_score",nullable = false, precision = 6,scale = 2)
    private BigDecimal finalScore;

    @Column(name = "fraud_prob",precision = 5,scale = 4)
    private BigDecimal fraudProb;

    @Column(nullable = false)
    private String outcome;

    @ManyToOne(fetch = FetchType.LAZY,optional = true)
    @JoinColumn(name = "lender_id",nullable = true,foreignKey = @ForeignKey(name = "fk_decisions_lender"))
    private Lender lender;

    @Column(name = "model_version",nullable = false)
    private String modelVersion;
    
    @Column(name = "shap_json",nullable = false,columnDefinition = "JSONB")
    private String shapJson;

    @Column(name = "decision_path",nullable = false,columnDefinition = "TEXT[]")
    private String[] decisionPath;

    @Column(name = "processing_ms",nullable = false)
    private Integer processingMs;

    @CreationTimestamp
    @Column(name = "decided_at",nullable = false,updatable = false,columnDefinition = "TIMESTAMPTZ DEFAULT now()")
    private OffsetDateTime decidedAt;

}
