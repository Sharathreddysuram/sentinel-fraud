package com.sharath.sentinel.detection.rules;

/**
 * Outcome of a single fraud rule evaluation.
 *
 * @param triggered whether the rule fired for this transaction
 * @param ruleName  stable rule identifier, e.g. "VELOCITY"
 * @param detail    human-readable explanation, shown on the alert
 * @param weight    contribution to the total risk score when triggered
 */
public record RuleResult(boolean triggered, String ruleName, String detail, int weight) {

    /** Convenience factory for the common "rule did not fire" case. */
    public static RuleResult notTriggered(String ruleName) {
        return new RuleResult(false, ruleName, "No anomaly detected", 0);
    }
}
