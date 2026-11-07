package com.sharath.sentinel.detection.api;

import com.sharath.sentinel.detection.alert.AlertService;
import com.sharath.sentinel.detection.api.dto.SimulateRequest;
import com.sharath.sentinel.detection.config.RuleProperties;
import com.sharath.sentinel.detection.events.TransactionEvent;
import com.sharath.sentinel.detection.rules.RulesEngine;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * Synchronous REST surface of the detection-service.
 *
 * <p>{@code /simulate} runs the exact same scoring path as the Kafka
 * consumer but synchronously and without infrastructure — the fastest way
 * to demo the service. Rule thresholds are readable and tunable at runtime
 * via {@code /rules}; alert case-management lives in the alert-service.
 */
@RestController
@RequestMapping("/api")
@Tag(name = "Detection", description = "Score transactions synchronously, tune rule thresholds")
public class SimulateController {

    private final RulesEngine rulesEngine;
    private final AlertService alertService;
    private final RuleProperties ruleProperties;

    public SimulateController(RulesEngine rulesEngine, AlertService alertService,
                              RuleProperties ruleProperties) {
        this.rulesEngine = rulesEngine;
        this.alertService = alertService;
        this.ruleProperties = ruleProperties;
    }

    @PostMapping("/simulate")
    @Operation(summary = "Score one transaction synchronously (no Kafka needed)",
            description = "Runs the rules engine immediately and persists a fraud alert "
                    + "in this service's database when the risk score crosses the threshold. "
                    + "Note: this path does NOT publish to the 'fraud-alerts' topic, so the "
                    + "alert-service read model is only fed by the Kafka consumer path.")
    public SimulateResponse simulate(@Valid @RequestBody SimulateRequest request) {
        TransactionEvent event = request.toEvent();
        RulesEngine.EvaluationResult result = rulesEngine.evaluate(event);

        UUID alertId = null;
        if (result.fraud()) {
            alertId = alertService.saveAlert(event, result).getId();
        }

        List<RuleDetail> details = result.triggeredRules().stream()
                .map(r -> new RuleDetail(r.ruleName(), r.detail(), r.weight()))
                .toList();

        return new SimulateResponse(
                event.getTransactionId(),
                result.riskScore(),
                result.fraud(),
                details,
                alertId);
    }

    @GetMapping("/rules")
    @Operation(summary = "Show the current rule thresholds")
    public RuleProperties getRules() {
        return ruleProperties;
    }

    @PutMapping("/rules")
    @Operation(summary = "Update rule thresholds at runtime",
            description = "Changes take effect on the next evaluation — no restart needed.")
    public RuleProperties updateRules(@Valid @RequestBody RuleProperties updated) {
        // Copy into the live bean so the running engine picks it up immediately.
        ruleProperties.setVelocity(updated.getVelocity());
        ruleProperties.setAmount(updated.getAmount());
        ruleProperties.setGeo(updated.getGeo());
        ruleProperties.setAlertThresholdScore(updated.getAlertThresholdScore());
        return ruleProperties;
    }

    /** Response for POST /api/simulate. */
    public record SimulateResponse(
            String transactionId,
            int riskScore,
            boolean fraud,
            List<RuleDetail> triggeredRules,
            UUID alertId) {
    }

    /** One fired rule, as returned in the simulate response. */
    public record RuleDetail(String ruleName, String detail, int weight) {
    }
}
