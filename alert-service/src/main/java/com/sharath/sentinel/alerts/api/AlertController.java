package com.sharath.sentinel.alerts.api;

import com.sharath.sentinel.alerts.view.AlertView;
import com.sharath.sentinel.alerts.view.AlertViewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.UUID;

/**
 * Analyst-facing REST API over the alert read model.
 *
 * <p>All data here arrives via the {@code fraud-alerts} Kafka topic — this
 * service never calls the detection-service directly. Rule thresholds are
 * owned by the detection-service; {@code GET /api/rules} documents the
 * defaults this read model was built against.
 */
@RestController
@RequestMapping("/api")
@Tag(name = "Alerts", description = "List, fetch and review fraud alerts (read model)")
public class AlertController {

    private final AlertViewService alertViewService;

    public AlertController(AlertViewService alertViewService) {
        this.alertViewService = alertViewService;
    }

    @GetMapping("/alerts")
    @Operation(summary = "List fraud alerts, newest first (paged)")
    public Page<AlertView> listAlerts(@PageableDefault(size = 20) Pageable pageable) {
        return alertViewService.listNewestFirst(pageable);
    }

    @GetMapping("/alerts/{alertId}")
    @Operation(summary = "Fetch one alert by id")
    public AlertView getAlert(@PathVariable UUID alertId) {
        return alertViewService.getById(alertId);
    }

    @PostMapping("/alerts/{alertId}/review")
    @Operation(summary = "Mark an alert as reviewed by an analyst")
    public AlertView reviewAlert(@PathVariable UUID alertId) {
        return alertViewService.markReviewed(alertId);
    }

    @GetMapping("/rules")
    @Operation(summary = "Rule thresholds this read model was built against",
            description = "Informational copy of the detection-service defaults. "
                    + "Live thresholds are owned by the detection-service — "
                    + "tune them there via PUT /api/rules on port 8092.")
    public Map<String, Object> rulesInfo() {
        return Map.of(
                "note", "Thresholds are owned by detection-service (port 8092). "
                        + "Tune them there via PUT /api/rules; this is a documented copy of the defaults.",
                "velocity", Map.of("maxTransactions", 5, "windowMinutes", 10, "weight", 40),
                "amount", Map.of("absoluteThreshold", 10000, "multiplierVsAverage", 3.0, "weight", 35),
                "geo", Map.of("windowMinutes", 60, "weight", 25),
                "alertThresholdScore", 50);
    }
}
