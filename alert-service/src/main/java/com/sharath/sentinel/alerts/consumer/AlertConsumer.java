package com.sharath.sentinel.alerts.consumer;

import com.sharath.sentinel.alerts.events.FraudAlertMessage;
import com.sharath.sentinel.alerts.view.AlertViewService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Consumes fraud alerts from the {@code fraud-alerts} topic and projects
 * them into this service's read-model database.
 *
 * <p>Projection is idempotent (alert id = primary key), so at-least-once
 * delivery from Kafka is safe. The body is guarded so one poisoned record
 * can never take the listener down.
 */
@Component
public class AlertConsumer {

    private static final Logger log = LoggerFactory.getLogger(AlertConsumer.class);

    private final AlertViewService alertViewService;

    public AlertConsumer(AlertViewService alertViewService) {
        this.alertViewService = alertViewService;
    }

    @KafkaListener(topics = "fraud-alerts", groupId = "sentinel-alerts")
    public void onFraudAlert(FraudAlertMessage message) {
        try {
            if (message == null || message.getAlertId() == null) {
                log.warn("Skipping malformed fraud alert message: {}", message);
                return;
            }
            alertViewService.project(message);
            log.info("Projected fraud alert {} (tx={}, score={}) into read model",
                    message.getAlertId(), message.getTransactionId(), message.getRiskScore());
        } catch (Exception ex) {
            // Never let one bad record kill the consumer loop.
            log.error("Error projecting fraud alert message, skipping: {}", message, ex);
        }
    }
}
