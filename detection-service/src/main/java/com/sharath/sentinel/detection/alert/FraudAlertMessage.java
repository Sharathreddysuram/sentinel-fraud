package com.sharath.sentinel.detection.alert;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Payload published to the Kafka topic {@code fraud-alerts} whenever a
 * transaction crosses the fraud threshold — consumed by the alert-service
 * (read model) and any downstream case-management / notification systems.
 *
 * <p>Explicit no-arg constructor + getters/setters so Jackson serialization
 * is obvious and never surprises anyone.
 */
public class FraudAlertMessage {

    private UUID alertId;
    private String transactionId;
    private String accountId;
    private BigDecimal amount;
    private int riskScore;
    private List<String> triggeredRules;
    private Instant createdAt;

    /** Required by Jackson. */
    public FraudAlertMessage() {
    }

    public FraudAlertMessage(UUID alertId, String transactionId, String accountId,
                             BigDecimal amount, int riskScore,
                             List<String> triggeredRules, Instant createdAt) {
        this.alertId = alertId;
        this.transactionId = transactionId;
        this.accountId = accountId;
        this.amount = amount;
        this.riskScore = riskScore;
        this.triggeredRules = triggeredRules;
        this.createdAt = createdAt;
    }

    public UUID getAlertId() {
        return alertId;
    }

    public void setAlertId(UUID alertId) {
        this.alertId = alertId;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }

    public String getAccountId() {
        return accountId;
    }

    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public int getRiskScore() {
        return riskScore;
    }

    public void setRiskScore(int riskScore) {
        this.riskScore = riskScore;
    }

    public List<String> getTriggeredRules() {
        return triggeredRules;
    }

    public void setTriggeredRules(List<String> triggeredRules) {
        this.triggeredRules = triggeredRules;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
