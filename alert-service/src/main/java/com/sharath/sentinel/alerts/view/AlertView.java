package com.sharath.sentinel.alerts.view;

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
 * Read-model projection of a fraud alert, built from the
 * {@code fraud-alerts} Kafka topic (CQRS-lite).
 *
 * <p>This is a SEPARATE table from the detection-service's
 * {@code fraud_alerts} — each service owns its database. The alert id is
 * reused as the primary key so the projection is idempotent: redelivered
 * messages simply overwrite the same row.
 */
@Entity
@Table(name = "alert_view")
public class AlertView {

    @Id
    private UUID alertId;

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
    private AlertViewStatus status;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    private Instant reviewedAt;

    protected AlertView() {
        // JPA
    }

    public AlertView(UUID alertId, String transactionId, String accountId,
                     BigDecimal amount, int riskScore, String triggeredRules,
                     Instant createdAt) {
        this.alertId = alertId;
        this.transactionId = transactionId;
        this.accountId = accountId;
        this.amount = amount;
        this.riskScore = riskScore;
        this.triggeredRules = triggeredRules;
        this.status = AlertViewStatus.OPEN;
        this.createdAt = createdAt;
    }

    public UUID getAlertId() {
        return alertId;
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

    public AlertViewStatus getStatus() {
        return status;
    }

    public void setStatus(AlertViewStatus status) {
        this.status = status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getReviewedAt() {
        return reviewedAt;
    }

    public void setReviewedAt(Instant reviewedAt) {
        this.reviewedAt = reviewedAt;
    }
}
