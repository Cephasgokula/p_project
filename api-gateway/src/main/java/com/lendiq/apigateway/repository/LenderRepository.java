package com.lendiq.apigateway.repository;

import com.lendiq.apigateway.entity.Lender;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.time.Instant;
import java.util.*;

@Repository
public interface LenderRepository extends JpaRepository<Lender, UUID> {

    // Interval Tree startup load — only active lenders are loaded into memory
    List<Lender> findByActiveTrue();

    // 30-day referral + approval analytics for GET /lenders/:id/stats
    @Query("""
        SELECT l.id, l.name,
               COUNT(d.id) AS referrals,
               SUM(CASE WHEN d.outcome = 'APPROVE' THEN 1 ELSE 0 END) AS approvals
        FROM Lender l
        LEFT JOIN Decision d ON d.lender.id = l.id AND d.decidedAt >= :since
        WHERE l.active = true
        GROUP BY l.id, l.name
    """)
    List<Object[]> findLenderStatsSince(Instant since);

    // Used after PUT /lenders/:id/rules to trigger Interval Tree rebuild
    Optional<Lender> findByIdAndActiveTrue(UUID id);
}
