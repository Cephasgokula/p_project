package com.lendiq.apigateway.entity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(
    name = "applications",
    indexes = {
        @Index(name = "idx_applications_applicant_id",columnList = "applicant_id"),
        @Index(name = "idx_applications_kafka_offset",columnList = "kafka_offset")
    }
)

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Application {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY,optional = false) // here we use the fetchLazy means load data when it is needed
    @JoinColumn(name = "applicant_id",nullable = false,foreignKey = @ForeignKey(name = "fk_applications_applicant"))
    private Applicant applicant;

    @Column(nullable = false,precision = 12,scale = 2)
    private BigDecimal amount;

    @Column(name = "term_months",nullable = false)
    private Integer termMonths;

    @Column(nullable = false)
    private String purpose;

    @Column(nullable = false)
    private String status;

    @Column(name = "source_channel",nullable = false)
    private String sourceChannel;

    @Column(name = "kafka_offset")
    private Long kafkaOffset;
    
    @CreationTimestamp
    @Column(name = "created_at",nullable = false,updatable = false,columnDefinition = "TIMESTAMPTZ DEFAULT now()")
    private OffsetDateTime createdAt;

}
