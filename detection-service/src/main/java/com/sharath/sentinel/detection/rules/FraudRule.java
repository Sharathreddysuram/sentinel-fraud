package com.sharath.sentinel.detection.rules;

import com.sharath.sentinel.detection.events.TransactionEvent;

/**
 * Strategy interface for fraud detection rules.
 *
 * <p>New rules plug in by implementing this interface and registering as a
 * Spring {@code @Component} — the {@link RulesEngine} picks up every
 * registered rule automatically, no wiring changes needed.
 */
public interface FraudRule {

    /**
     * Evaluate one transaction against the account's recent history.
     *
     * @param event   the incoming transaction (not yet recorded in history)
     * @param history the account's recent transactions, ordered oldest-first
     * @return the rule's verdict
     */
    RuleResult evaluate(TransactionEvent event, AccountHistory history);
}
