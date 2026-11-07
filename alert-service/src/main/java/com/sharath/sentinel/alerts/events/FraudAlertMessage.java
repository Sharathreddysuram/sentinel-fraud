package com.sharath.sentinel.alerts.events;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Fraud alert as published on the Kafka topic {@code fraud-alerts} by the
 * detection-service.
 *
 * <p>The schema is intentionally duplicated per service so each service owns
 * its contract. Explicit no-arg constructor + getters/setters so Jackson
 * deserialization is obvious.
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
