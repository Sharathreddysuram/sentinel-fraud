package com.sharath.sentinel.detection.rules;

import com.sharath.sentinel.detection.config.RuleProperties;
import com.sharath.sentinel.detection.events.TransactionEvent;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * Runs every registered {@link FraudRule} against one transaction and
 * combines the results into a risk score.
 *
 * <p>Ordering matters: the account's history is snapshotted BEFORE the
 * current event is recorded, so a transaction is never scored against
 * itself. The event is recorded afterwards so the NEXT evaluation sees it.
 */
@Component
public class RulesEngine {

    /** Longest window any rule looks back — keeps one history snapshot cheap. */
    private static final Duration MAX_RULE_WINDOW = Duration.ofHours(24);

    private final List<FraudRule> rules;
    private final RuleProperties properties;
    private final TransactionHistoryStore historyStore;

    public RulesEngine(List<FraudRule> rules, RuleProperties properties,
                       TransactionHistoryStore historyStore) {
        this.rules = rules;
        this.properties = properties;
        this.historyStore = historyStore;
    }

    public EvaluationResult evaluate(TransactionEvent event) {
        // 1. Snapshot history WITHOUT the current event.
        AccountHistory history = historyStore.historyFor(event.getAccountId(), MAX_RULE_WINDOW);

        // 2. Run every registered rule against that snapshot.
        List<RuleResult> triggered = new ArrayList<>();
        for (FraudRule rule : rules) {
            RuleResult result = rule.evaluate(event, history);
            if (result.triggered()) {
                triggered.add(result);
            }
        }

        // 3. Score = sum of the weights of the rules that fired.
        int riskScore = triggered.stream().mapToInt(RuleResult::weight).sum();
        boolean fraud = riskScore >= properties.getAlertThresholdScore();

        // 4. Record the event so future evaluations see it.
        historyStore.record(event);

        return new EvaluationResult(riskScore, List.copyOf(triggered), fraud);
    }

    /**
     * @param riskScore      sum of triggered rule weights (0-100 scale by default)
     * @param triggeredRules rules that fired, in evaluation order
     * @param fraud          true when riskScore meets the alert threshold
     */
    public record EvaluationResult(int riskScore, List<RuleResult> triggeredRules, boolean fraud) {
    }
}
