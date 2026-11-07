package com.sharath.sentinel.alerts.view;

import com.sharath.sentinel.alerts.common.ResourceNotFoundException;
import com.sharath.sentinel.alerts.events.FraudAlertMessage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

/**
 * Application service for the alert read model: project incoming Kafka
 * messages into {@link AlertView} rows, and serve the analyst API.
 */
@Service
public class AlertViewService {

    private final AlertViewRepository repository;

    public AlertViewService(AlertViewRepository repository) {
        this.repository = repository;
    }

    /**
     * Project a fraud-alert message into the read model.
     *
     * <p>Idempotent: the alert id is the primary key, so a redelivered
     * message overwrites the same row instead of creating a duplicate.
     */
    @Transactional
    public AlertView project(FraudAlertMessage message) {
        String rules = message.getTriggeredRules() == null
                ? ""
                : String.join(",", message.getTriggeredRules());
        AlertView view = new AlertView(
                message.getAlertId(),
                message.getTransactionId(),
                message.getAccountId(),
                message.getAmount(),
                message.getRiskScore(),
                rules,
                message.getCreatedAt() != null ? message.getCreatedAt() : Instant.now());
        return repository.save(view);
    }

    @Transactional(readOnly = true)
    public Page<AlertView> listNewestFirst(Pageable pageable) {
        return repository.findAllByOrderByCreatedAtDesc(pageable);
    }

    @Transactional(readOnly = true)
    public AlertView getById(UUID alertId) {
        return repository.findById(alertId)
                .orElseThrow(() -> new ResourceNotFoundException("Alert", alertId.toString()));
    }

    /** Mark an alert reviewed after an analyst has looked at it. */
    @Transactional
    public AlertView markReviewed(UUID alertId) {
        AlertView view = getById(alertId);
        view.setStatus(AlertViewStatus.REVIEWED);
        view.setReviewedAt(Instant.now());
        return repository.save(view);
    }
}
