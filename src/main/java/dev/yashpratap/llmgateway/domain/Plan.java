package dev.yashpratap.llmgateway.domain;

/**
 * Subscription plan tiers that control rate limits and budget.
 *
 * <ul>
 *   <li>FREE — 10 requests per minute</li>
 *   <li>PRO — 60 requests per minute</li>
 *   <li>ENTERPRISE — 300 requests per minute</li>
 * </ul>
 */
public enum Plan {
    FREE, PRO, ENTERPRISE
}
