package com.lendiq.apigateway.dsa;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Red-Black Tree implementation from scratch.
 * Used to maintain a sorted lender queue by match score.
 * Guarantees O(log n) insert, delete, and max operations.
 *
 * Properties maintained:
 * 1. Every node is RED or BLACK
 * 2. Root is always BLACK
 * 3. No two consecutive RED nodes
 * 4. Every path from root to null has the same black-height
 */
@Slf4j
@Component
public class RedBlackTree {

    private static final boolean RED = true;
    private static final boolean BLACK = false;

    public static class RBNode {
        public double score;
        public UUID lenderId;
        public String lenderName;
        boolean color;
        RBNode left, right, parent;

        RBNode(double score, UUID lenderId, String lenderName) {
            this.score = score;
            this.lenderId = lenderId;
            this.lenderName = lenderName;
            this.color = RED; // new nodes always start RED
        }
    }

    private RBNode root;
    private int size;

    public RedBlackTree() {
        this.root = null;
        this.size = 0;
    }

    /**
     * Insert a lender with a given match score. O(log n).
     */
    public void insert(UUID lenderId, String lenderName, double matchScore) {
        RBNode newNode = new RBNode(matchScore, lenderId, lenderName);

        if (root == null) {
            root = newNode;
            root.color = BLACK;
            size++;
            return;
        }

        // Standard BST insert
        RBNode current = root;
        RBNode parent = null;
        while (current != null) {
            parent = current;
            if (matchScore < current.score) {
                current = current.left;
            } else if (matchScore > current.score) {
                current = current.right;
            } else {
                // Tie-break by lender ID to allow multiple entries with same score
                if (lenderId.compareTo(current.lenderId) < 0) {
                    current = current.left;
                } else {
                    current = current.right;
                }
            }
        }

        newNode.parent = parent;
        if (matchScore < parent.score || (matchScore == parent.score && lenderId.compareTo(parent.lenderId) < 0)) {
            parent.left = newNode;
        } else {
            parent.right = newNode;
        }

        size++;
        fixInsert(newNode);
    }

    /**
     * Get the lender with the highest match score. O(log n).
     */
    public RBNode getMax() {
        if (root == null) return null;
        RBNode current = root;
        while (current.right != null) {
            current = current.right;
        }
        return current;
    }

    /**
     * Delete a lender by ID. O(log n).
     */
    public boolean delete(UUID lenderId) {
        RBNode node = findByLenderId(root, lenderId);
        if (node == null) return false;
        deleteNode(node);
        size--;
        return true;
    }

    private RBNode findByLenderId(RBNode node, UUID lenderId) {
        if (node == null) return null;
        if (node.lenderId.equals(lenderId)) return node;

        RBNode found = findByLenderId(node.left, lenderId);
        if (found != null) return found;
        return findByLenderId(node.right, lenderId);
    }

    // ── Red-Black fix-up after insert ──

    private void fixInsert(RBNode node) {
        while (node != root && node.parent.color == RED) {
            if (node.parent == node.parent.parent.left) {
                RBNode uncle = node.parent.parent.right;
                if (uncle != null && uncle.color == RED) {
                    // Case 1: Uncle is RED — recolor
                    node.parent.color = BLACK;
                    uncle.color = BLACK;
                    node.parent.parent.color = RED;
                    node = node.parent.parent;
                } else {
                    if (node == node.parent.right) {
                        // Case 2: Node is right child — left rotate
                        node = node.parent;
                        rotateLeft(node);
                    }
                    // Case 3: Node is left child — right rotate
                    node.parent.color = BLACK;
                    node.parent.parent.color = RED;
                    rotateRight(node.parent.parent);
                }
            } else {
                // Mirror cases
                RBNode uncle = node.parent.parent.left;
                if (uncle != null && uncle.color == RED) {
                    node.parent.color = BLACK;
                    uncle.color = BLACK;
                    node.parent.parent.color = RED;
                    node = node.parent.parent;
                } else {
                    if (node == node.parent.left) {
                        node = node.parent;
                        rotateRight(node);
                    }
                    node.parent.color = BLACK;
                    node.parent.parent.color = RED;
                    rotateLeft(node.parent.parent);
                }
            }
        }
        root.color = BLACK;
    }

    // ── Deletion ──

    private void deleteNode(RBNode z) {
        RBNode y = z;
        RBNode x;
        boolean yOriginalColor = y.color;

        if (z.left == null) {
            x = z.right;
            transplant(z, z.right);
        } else if (z.right == null) {
            x = z.left;
            transplant(z, z.left);
        } else {
            // Find in-order successor
            y = minimum(z.right);
            yOriginalColor = y.color;
            x = y.right;

            if (y.parent == z) {
                if (x != null) x.parent = y;
            } else {
                transplant(y, y.right);
                y.right = z.right;
                if (y.right != null) y.right.parent = y;
            }
            transplant(z, y);
            y.left = z.left;
            if (y.left != null) y.left.parent = y;
            y.color = z.color;
        }

        if (yOriginalColor == BLACK && x != null) {
            fixDelete(x);
        }
    }

    private void fixDelete(RBNode x) {
        while (x != root && x.color == BLACK) {
            if (x == x.parent.left) {
                RBNode w = x.parent.right;
                if (w != null && w.color == RED) {
                    w.color = BLACK;
                    x.parent.color = RED;
                    rotateLeft(x.parent);
                    w = x.parent.right;
                }
                if (w == null) break;
                if ((w.left == null || w.left.color == BLACK) &&
                    (w.right == null || w.right.color == BLACK)) {
                    w.color = RED;
                    x = x.parent;
                } else {
                    if (w.right == null || w.right.color == BLACK) {
                        if (w.left != null) w.left.color = BLACK;
                        w.color = RED;
                        rotateRight(w);
                        w = x.parent.right;
                    }
                    if (w != null) {
                        w.color = x.parent.color;
                        if (w.right != null) w.right.color = BLACK;
                    }
                    x.parent.color = BLACK;
                    rotateLeft(x.parent);
                    x = root;
                }
            } else {
                RBNode w = x.parent.left;
                if (w != null && w.color == RED) {
                    w.color = BLACK;
                    x.parent.color = RED;
                    rotateRight(x.parent);
                    w = x.parent.left;
                }
                if (w == null) break;
                if ((w.right == null || w.right.color == BLACK) &&
                    (w.left == null || w.left.color == BLACK)) {
                    w.color = RED;
                    x = x.parent;
                } else {
                    if (w.left == null || w.left.color == BLACK) {
                        if (w.right != null) w.right.color = BLACK;
                        w.color = RED;
                        rotateLeft(w);
                        w = x.parent.left;
                    }
                    if (w != null) {
                        w.color = x.parent.color;
                        if (w.left != null) w.left.color = BLACK;
                    }
                    x.parent.color = BLACK;
                    rotateRight(x.parent);
                    x = root;
                }
            }
        }
        x.color = BLACK;
    }

    // ── Rotations ──

    private void rotateLeft(RBNode x) {
        RBNode y = x.right;
        x.right = y.left;
        if (y.left != null) y.left.parent = x;
        y.parent = x.parent;
        if (x.parent == null) {
            root = y;
        } else if (x == x.parent.left) {
            x.parent.left = y;
        } else {
            x.parent.right = y;
        }
        y.left = x;
        x.parent = y;
    }

    private void rotateRight(RBNode x) {
        RBNode y = x.left;
        x.left = y.right;
        if (y.right != null) y.right.parent = x;
        y.parent = x.parent;
        if (x.parent == null) {
            root = y;
        } else if (x == x.parent.right) {
            x.parent.right = y;
        } else {
            x.parent.left = y;
        }
        y.right = x;
        x.parent = y;
    }

    private void transplant(RBNode u, RBNode v) {
        if (u.parent == null) {
            root = v;
        } else if (u == u.parent.left) {
            u.parent.left = v;
        } else {
            u.parent.right = v;
        }
        if (v != null) v.parent = u.parent;
    }

    private RBNode minimum(RBNode node) {
        while (node.left != null) node = node.left;
        return node;
    }

    public int size() {
        return size;
    }

    public void clear() {
        root = null;
        size = 0;
    }
}
