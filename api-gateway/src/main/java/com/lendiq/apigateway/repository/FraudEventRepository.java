package com.lendiq.apigateway.repository;

import com.lendiq.apigateway.entity.FraudEvent;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.stereotype.Repository;
import java.util.*;

@Repository
public interface FraudEventRepository
        extends JpaRepository<FraudEvent, UUID>,
                JpaSpecificationExecutor<FraudEvent> {

    // Admin queue: unresolved flags, newest first
    Page<FraudEvent> findByResolvedFalseOrderByDetectedAtDesc(Pageable pageable);

    // Filter by event type: velocity / gnn_ring / manual_flag
    Page<FraudEvent> findByEventTypeAndResolvedFalse(String eventType, Pageable pageable);

    // Pre-routing check: block if applicant has any active unresolved flags
    boolean existsByApplicantIdAndResolvedFalse(UUID applicantId);

    // Fraud ring detail: all events sharing a GNN ring ID (for the D3 visualisation)
    List<FraudEvent> findByRingId(String ringId);

    // Resolve — the ONLY allowed mutation on this table
    @Modifying
    @Query("UPDATE FraudEvent fe SET fe.resolved = true WHERE fe.id = :id")
    int resolveFlag(UUID id);
}