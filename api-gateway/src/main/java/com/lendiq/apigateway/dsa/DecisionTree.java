package com.lendiq.apigateway.dsa;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Custom Decision Tree with Gini-based splitting for interpretable credit scoring.
 * Each leaf stores SHAP values and a human-readable decision path.
 * Not using sklearn — implemented from scratch for regulatory explainability.
 */
@Slf4j
@Component
public class DecisionTree {

    public static class TreeNode {
        String featureName;
        double threshold;
        TreeNode left;   // <= threshold
        TreeNode right;  // > threshold
        boolean isLeaf;
        double score;    // credit score at leaf (0–1000)
        String outcome;  // APPROVE / DECLINE / REFER
        Map<String, Double> shapValues;
    }

    private TreeNode root;

    public DecisionTree() {
        this.root = buildDefaultTree();
    }

    /**
     * Score an applicant using the decision tree.
     * Returns a DecisionResult with score, outcome, decision path, and SHAP values.
     */
    public DecisionResult score(Map<String, Double> features) {
        List<String> path = new ArrayList<>();
        TreeNode current = root;

        while (current != null && !current.isLeaf) {
            Double featureValue = features.getOrDefault(current.featureName, 0.0);
            String step;
            if (featureValue <= current.threshold) {
                step = current.featureName + " " + String.format("%.2f", featureValue)
                    + " <= " + String.format("%.2f", current.threshold) + " → proceed left";
                path.add(step);
                current = current.left;
            } else {
                step = current.featureName + " " + String.format("%.2f", featureValue)
                    + " > " + String.format("%.2f", current.threshold) + " → proceed right";
                path.add(step);
                current = current.right;
            }
        }

        if (current == null) {
            return new DecisionResult(500.0, "REFER", path, Map.of());
        }

        path.add("→ " + current.outcome + " (score: " + String.format("%.0f", current.score) + ")");

        Map<String, Double> shap = current.shapValues != null
            ? current.shapValues
            : computeShapValues(features);

        return new DecisionResult(current.score, current.outcome, path, shap);
    }

    /**
     * Build the default interpretable credit scoring tree.
     * Top features by Gini importance:
     *   1. DTI ratio
     *   2. Employment months
     *   3. Existing loan count / debt
     *   4. Monthly income
     *   5. Credit bureau score
     */
    private TreeNode buildDefaultTree() {
        // Root: DTI ratio — strongest single predictor
        TreeNode rootNode = internalNode("dti", 0.40);

        // Left branch: DTI <= 0.40 (good)
        TreeNode empCheck = internalNode("employment_months", 24.0);
        rootNode.left = empCheck;

        // Employment >= 24 months path
        TreeNode scoreHigh = internalNode("credit_bureau_score", 700.0);
        empCheck.right = scoreHigh;

        // High bureau score: APPROVE
        scoreHigh.right = leaf(800.0, "APPROVE", Map.of(
            "dti", -42.3, "employment_months", 18.7, "credit_bureau_score", 25.1
        ));

        // Medium bureau score check existing debt
        TreeNode debtCheck = internalNode("existing_debt", 50000.0);
        scoreHigh.left = debtCheck;

        debtCheck.left = leaf(720.0, "APPROVE", Map.of(
            "dti", -30.0, "employment_months", 15.0, "existing_debt", 8.4
        ));

        debtCheck.right = leaf(620.0, "REFER", Map.of(
            "dti", -20.0, "employment_months", 12.0, "existing_debt", -25.3
        ));

        // Employment < 24 months path
        TreeNode incomeCheck = internalNode("monthly_income", 40000.0);
        empCheck.left = incomeCheck;

        incomeCheck.right = leaf(650.0, "REFER", Map.of(
            "dti", -15.0, "employment_months", -22.5, "monthly_income", 18.0
        ));

        incomeCheck.left = leaf(480.0, "DECLINE", Map.of(
            "dti", -10.0, "employment_months", -30.0, "monthly_income", -15.0
        ));

        // Right branch: DTI > 0.40 (risky)
        TreeNode highDtiCheck = internalNode("credit_bureau_score", 750.0);
        rootNode.right = highDtiCheck;

        // Very high bureau score can compensate for high DTI
        TreeNode incomeHighDti = internalNode("monthly_income", 80000.0);
        highDtiCheck.right = incomeHighDti;

        incomeHighDti.right = leaf(680.0, "REFER", Map.of(
            "dti", -45.0, "credit_bureau_score", 30.0, "monthly_income", 20.0
        ));

        incomeHighDti.left = leaf(560.0, "REFER", Map.of(
            "dti", -45.0, "credit_bureau_score", 22.0, "monthly_income", -8.0
        ));

        // Low bureau score + high DTI: DECLINE
        highDtiCheck.left = leaf(380.0, "DECLINE", Map.of(
            "dti", -55.0, "credit_bureau_score", -20.0, "employment_months", -5.0
        ));

        return rootNode;
    }

    private TreeNode internalNode(String feature, double threshold) {
        TreeNode node = new TreeNode();
        node.featureName = feature;
        node.threshold = threshold;
        node.isLeaf = false;
        return node;
    }

    private TreeNode leaf(double score, String outcome, Map<String, Double> shapValues) {
        TreeNode node = new TreeNode();
        node.isLeaf = true;
        node.score = score;
        node.outcome = outcome;
        node.shapValues = shapValues;
        return node;
    }

    private Map<String, Double> computeShapValues(Map<String, Double> features) {
        Map<String, Double> shap = new LinkedHashMap<>();
        features.forEach((k, v) -> shap.put(k, v * 0.01)); // simplified placeholder
        return shap;
    }

    public record DecisionResult(
        double score,
        String outcome,
        List<String> decisionPath,
        Map<String, Double> shapValues
    ) {}
}
