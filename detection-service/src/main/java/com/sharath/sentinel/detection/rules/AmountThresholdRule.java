package com.sharath.sentinel.detection.rules;

import com.sharath.sentinel.detection.config.RuleProperties;
import com.sharath.sentinel.detection.events.TransactionEvent;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * AMOUNT — fires when a transaction is either absolutely large
 * (at/above {@code absolute-threshold}) or abnormally large for THIS
 * account (>= {@code multiplier-vs-average} x the account's recent
 * average, requiring at least 3 historical events for a stable average).
 */
@Component
public class AmountThresholdRule implements FraudRule {

    private static final int MIN_HISTORY_FOR_AVERAGE = 3;

    private final RuleProperties properties;

    public AmountThresholdRule(RuleProperties properties) {
        this.properties = properties;
    }

    @Override
    public RuleResult evaluate(TransactionEvent event, AccountHistory history) {
        BigDecimal amount = event.getAmount();
        if (amount == null) {
            return RuleResult.notTriggered("AMOUNT");
        }

        BigDecimal absoluteThreshold = properties.getAmount().getAbsoluteThreshold();
        int weight = properties.getAmount().getWeight();

        if (amount.compareTo(absoluteThreshold) >= 0) {
            return new RuleResult(true, "AMOUNT",
                    "Transaction amount " + amount + " meets/exceeds absolute threshold "
                            + absoluteThreshold,
                    weight);
        }

        List<TransactionEvent> past = history.recentEvents().stream()
                .filter(e -> e.getAmount() != null)
                .toList();
        if (past.size() >= MIN_HISTORY_FOR_AVERAGE) {
            BigDecimal average = past.stream()
                    .map(TransactionEvent::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add)
                    .divide(BigDecimal.valueOf(past.size()), 2, RoundingMode.HALF_UP);
            BigDecimal limit = average.multiply(
                    BigDecimal.valueOf(properties.getAmount().getMultiplierVsAverage()));
            if (amount.compareTo(limit) >= 0) {
                return new RuleResult(true, "AMOUNT",
                        "Transaction amount " + amount + " is >= "
                                + properties.getAmount().getMultiplierVsAverage()
                                + "x the account's recent average of " + average,
                        weight);
            }
        }

        return RuleResult.notTriggered("AMOUNT");
    }
}
