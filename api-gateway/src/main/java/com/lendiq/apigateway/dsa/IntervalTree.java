package com.lendiq.apigateway.dsa;

import com.lendiq.apigateway.entity.Lender;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Augmented Interval Tree for lender eligibility matching.
 * Each lender defines an income range [incomeMin, incomeMax].
 * Given an applicant's income, query returns all lenders whose range contains that value.
 * O(log n + k) query time where k = number of matching intervals.
 */
@Slf4j
@Component
public class IntervalTree {

    private static class IntervalNode {
        double low;
        double high;
        double maxHigh; // max high in subtree
        Lender lender;
        IntervalNode left;
        IntervalNode right;

        IntervalNode(double low, double high, Lender lender) {
            this.low = low;
            this.high = high;
            this.maxHigh = high;
            this.lender = lender;
        }
    }

    // Atomic reference for lock-free swap during rebuild
    private final AtomicReference<IntervalNode> root = new AtomicReference<>(null);

    /**
     * Rebuild the entire tree from a list of active lenders.
     * Uses sorted bulk-load for O(n log n) balanced construction.
     */
    public void rebuild(List<Lender> lenders) {
        if (lenders == null || lenders.isEmpty()) {
            root.set(null);
            return;
        }

        List<Lender> sorted = new ArrayList<>(lenders);
        sorted.sort((a, b) -> a.getIncomeMin().compareTo(b.getIncomeMin()));

        IntervalNode newRoot = buildBalanced(sorted, 0, sorted.size() - 1);
        root.set(newRoot); // atomic swap — zero downtime
        log.info("Interval Tree rebuilt with {} lenders", lenders.size());
    }

    private IntervalNode buildBalanced(List<Lender> lenders, int start, int end) {
        if (start > end) return null;

        int mid = start + (end - start) / 2;
        Lender lender = lenders.get(mid);
        IntervalNode node = new IntervalNode(
            lender.getIncomeMin().doubleValue(),
            lender.getIncomeMax().doubleValue(),
            lender
        );

        node.left = buildBalanced(lenders, start, mid - 1);
        node.right = buildBalanced(lenders, mid + 1, end);

        // Update maxHigh from children
        node.maxHigh = node.high;
        if (node.left != null) node.maxHigh = Math.max(node.maxHigh, node.left.maxHigh);
        if (node.right != null) node.maxHigh = Math.max(node.maxHigh, node.right.maxHigh);

        return node;
    }

    /**
     * Stabbing query: find all lenders whose income range contains the given value.
     */
    public List<Lender> query(BigDecimal income) {
        IntervalNode currentRoot = root.get();
        if (currentRoot == null || income == null) return Collections.emptyList();

        List<Lender> results = new ArrayList<>();
        queryHelper(currentRoot, income.doubleValue(), results);
        return results;
    }

    private void queryHelper(IntervalNode node, double point, List<Lender> out) {
        if (node == null) return;

        // Prune: if the max high in this subtree is less than our point, no match possible
        if (node.maxHigh < point) return;

        // Check left subtree
        queryHelper(node.left, point, out);

        // Check current node interval
        if (node.low <= point && point <= node.high) {
            out.add(node.lender);
        }

        // Optimization: if point < low, no need to check right subtree
        // (since right subtree has higher low values in a sorted build)
        if (point >= node.low) {
            queryHelper(node.right, point, out);
        }
    }

    /**
     * Multi-dimensional query: filter lenders matching income, age, and score thresholds.
     */
    public List<Lender> queryEligible(BigDecimal income, int age, BigDecimal score, BigDecimal loanAmount) {
        List<Lender> incomeMatches = query(income);
        return incomeMatches.stream()
            .filter(l -> age >= l.getAgeMin() && age <= l.getAgeMax())
            .filter(l -> score.compareTo(l.getScoreThreshold()) >= 0)
            .filter(l -> loanAmount.compareTo(l.getMaxLoanAmount()) <= 0)
            .filter(Lender::isActive)
            .toList();
    }

    public int size() {
        return countNodes(root.get());
    }

    private int countNodes(IntervalNode node) {
        if (node == null) return 0;
        return 1 + countNodes(node.left) + countNodes(node.right);
    }
}
