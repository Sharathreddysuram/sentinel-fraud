package com.sharath.sentinel.detection.alert;

import com.sharath.sentinel.detection.events.TransactionEvent;
import com.sharath.sentinel.detection.rules.RuleResult;
import com.sharath.sentinel.detection.rules.RulesEngine;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.stream.Collectors;

/**
 * Application service for fraud alerts in the detection-service's own
 * write-model database.
 */
@Service
public class AlertService {

    private final FraudAlertRepository repository;

    public AlertService(FraudAlertRepository repository) {
        this.repository = repository;
    }

    /** Persist an alert for a fraudulent evaluation and return the saved entity. */
    @Transactional
    public FraudAlert saveAlert(TransactionEvent event, RulesEngine.EvaluationResult result) {
        String rules = result.triggeredRules().stream()
                .map(RuleResult::ruleName)
                .collect(Collectors.joining(","));
        FraudAlert alert = new FraudAlert(
                event.getTransactionId(),
                event.getAccountId(),
                event.getAmount(),
                result.riskScore(),
                rules);
        return repository.save(alert);
    }
}
