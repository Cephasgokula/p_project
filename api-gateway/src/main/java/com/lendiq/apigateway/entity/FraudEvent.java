package com.lendiq.apigateway.entity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "fraud_events")

@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class FraudEvent {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false,nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY,optional = false)
    @JoinColumn(name = "applicant_id",nullable = false,foreignKey = @ForeignKey(name = "fraud_event_applicant"))
    private Applicant applicant;

    @Column(name = "event_type",nullable = false)
    private String eventType;

    @Column(name = "window_count")
    private Integer windowCount;

    @Column(name = "ring_id")
    private String ringId;

    @Column(name = "fraud_prob",precision = 5,scale = 4)
    private BigDecimal fraudProb;

    @Column(nullable = false, columnDefinition = "BOOLEAN DEFAULT false")
    private boolean resolved = false;

    @CreationTimestamp
    @Column(name = "detected_at",nullable = false,updatable = false,columnDefinition = "TIMESTAMPTZ DEFAULT now()")
    private OffsetDateTime detectedAt;

    
}
