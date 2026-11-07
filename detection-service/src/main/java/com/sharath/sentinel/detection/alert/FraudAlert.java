package com.sharath.sentinel.detection.alert;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * A persisted fraud alert — one row per transaction whose risk score
 * crossed the alert threshold. This is the detection-service's own
 * write-model table; the alert-service builds a separate read model
 * from the {@code fraud-alerts} Kafka topic.
 */
@Entity
@Table(name = "fraud_alerts")
public class FraudAlert {

    @Id
    private UUID id;

    @Column(nullable = false)
    private String transactionId;

    @Column(nullable = false)
    private String accountId;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false)
    private int riskScore;

    /** Rule names that fired, comma-joined (e.g. "VELOCITY,GEO"). */
    @Column(length = 500)
    private String triggeredRules;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AlertStatus status;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    protected FraudAlert() {
        // JPA
    }

    public FraudAlert(String transactionId, String accountId, BigDecimal amount,
                      int riskScore, String triggeredRules) {
        this.id = UUID.randomUUID();
        this.transactionId = transactionId;
        this.accountId = accountId;
        this.amount = amount;
        this.riskScore = riskScore;
        this.triggeredRules = triggeredRules;
        this.status = AlertStatus.OPEN;
        this.createdAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public String getAccountId() {
        return accountId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public int getRiskScore() {
        return riskScore;
    }

    public String getTriggeredRules() {
        return triggeredRules;
    }

    public AlertStatus getStatus() {
        return status;
    }

    public void setStatus(AlertStatus status) {
        this.status = status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
