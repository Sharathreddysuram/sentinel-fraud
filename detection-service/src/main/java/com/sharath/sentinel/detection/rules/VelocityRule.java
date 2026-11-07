package com.sharath.sentinel.detection.rules;

import com.sharath.sentinel.detection.config.RuleProperties;
import com.sharath.sentinel.detection.events.TransactionEvent;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;

/**
 * VELOCITY — catches card-testing / account-takeover bursts: many small
 * transactions pushed through in a short window (e.g. 5+ in 10 minutes).
 */
@Component
public class VelocityRule implements FraudRule {

    private final RuleProperties properties;

    public VelocityRule(RuleProperties properties) {
        this.properties = properties;
    }

    @Override
    public RuleResult evaluate(TransactionEvent event, AccountHistory history) {
        int maxTransactions = properties.getVelocity().getMaxTransactions();
        Duration window = Duration.ofMinutes(properties.getVelocity().getWindowMinutes());

        // +1 for the event currently being evaluated (not in history yet).
        int countInWindow = history.eventsInWindow(window, Instant.now()).size() + 1;

        if (countInWindow >= maxTransactions) {
            return new RuleResult(true, "VELOCITY",
                    "Account " + event.getAccountId() + " made " + countInWindow
                            + " transactions within " + window.toMinutes()
                            + " minutes (limit " + maxTransactions + ")",
                    properties.getVelocity().getWeight());
        }
        return RuleResult.notTriggered("VELOCITY");
    }
}
