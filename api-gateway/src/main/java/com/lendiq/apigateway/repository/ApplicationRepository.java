package com.lendiq.apigateway.repository;

import com.lendiq.apigateway.entity.Application;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.stereotype.Repository;
import java.time.Instant;
import java.util.*;

@Repository
public interface ApplicationRepository
        extends JpaRepository<Application, UUID>,
                JpaSpecificationExecutor<Application> {

    // Status polling by applicant — GET /applications list
    Page<Application> findByApplicantIdOrderByCreatedAtDesc(UUID applicantId, Pageable pageable);

    // Exactly-once deduplication: check if this Kafka offset was already consumed
    boolean existsByKafkaOffset(long kafkaOffset);

    // Batch scoring: pull up to 100 pending apps in arrival order
    List<Application> findTop100ByStatusOrderByCreatedAtAsc(String status);

    // Audit export: all applications in a date range for regulatory CSV
    @Query("""
        SELECT ap FROM Application ap
        WHERE ap.createdAt BETWEEN :from AND :to
        ORDER BY ap.createdAt ASC
    """)
    List<Application> findByCreatedAtBetween(Instant from, Instant to);

    // Admin dashboard tiles: count by status
    @Query("SELECT ap.status, COUNT(ap) FROM Application ap GROUP BY ap.status")
    List<Object[]> countGroupByStatus();

    // Ownership-aware lookup: applicant can only fetch their own record
    Optional<Application> findByIdAndApplicantId(UUID id, UUID applicantId);
}