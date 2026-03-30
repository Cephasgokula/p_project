package com.lendiq.apigateway.entity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Check;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(
    name = "applicants",
    indexes = {
        @Index(name = "idx_applicants_device_fp",columnList = "device_fp"),
        @Index(name = "idx_applicants_ip_hash",columnList = "ip_hash")
    }
)

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Applicant {
    

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false,nullable = false)
    private UUID id;

    @Column(name = "full_name", nullable = false)
    private String fullName;

    @Column(name = "pan_hash",nullable = false,unique = true)
    private String panHash;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal income;

    @Check(constraints = "age > 17")
    @Column
    private Integer age;

    @Column(name = "employment_months", nullable = false)
    private Integer employmentMonths;

    @Builder.Default
    @Column(name = "existing_debt", precision = 12,scale = 2)
    private BigDecimal existingDebt = BigDecimal.ZERO;

    @Column(name = "credit_bureau_score")
    private Integer creditBreauScore;

    @Column(name = "device_fp")
    private String deviceFp;

    @Column(name = "ip_hash")
    private String ipHash;

    @CreationTimestamp
    @Column(name = "created_at",nullable = false,updatable = false,columnDefinition = "TIMESTAMPTZ DEFAULT now()")
    private OffsetDateTime createdAt;
}
