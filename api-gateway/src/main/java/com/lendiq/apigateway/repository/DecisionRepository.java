package com.lendiq.apigateway.repository;

import com.lendiq.apigateway.entity.Decision;
import org.springframework.data.jpa.repository.*;
import org.springframework.stereotype.Repository;
import java.time.Instant;
import java.util.*;

@Repository
public interface DecisionRepository extends JpaRepository<Decision, UUID> {

    // Primary audit trail lookup
    Optional<Decision> findByApplicationId(UUID applicationId);

    // Admin dashboard: latest 50 decisions for the timeline widget
    List<Decision> findTop50ByOrderByDecidedAtDesc();

    // ML retraining: decisions since a cutoff for PSI drift calculation
    @Query("SELECT d FROM Decision d WHERE d.decidedAt >= :since AND d.modelVersion = :version")
    List<Decision> findByModelVersionSince(String version, Instant since);

    // Fairness monitoring: aggregate approval rates for a time window
    @Query("""
        SELECT d.outcome, COUNT(d)
        FROM Decision d
        WHERE d.decidedAt >= :since
        GROUP BY d.outcome
    """)
    List<Object[]> countOutcomesSince(Instant since);

    // IMPORTANT: No @Modifying update/delete methods defined here.
    // The decisions table is append-only — this interface enforces it.
}
